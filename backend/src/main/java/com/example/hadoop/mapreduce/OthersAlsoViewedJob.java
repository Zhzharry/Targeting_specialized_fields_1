package com.example.hadoop.mapreduce;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.*;

/**
 * 其他用户也在看推荐MapReduce作业 (Java 8 兼容版本)
 * 
 * 输入1（用户相似度）：userId1,userId2\tsimilarityScore
 * 输入2（用户行为）：userId\tpropertyId\tbehaviorType\tweight\ttimestamp
 * 输出：targetUserId\tpropertyId\trecommendationScore\treason
 * 
 * 位置：backend/src/main/java/com/example/hadoop/mapreduce/OthersAlsoViewedJob.java
 */
public class OthersAlsoViewedJob extends Configured implements Tool {

    private static final String SIMILARITY_PREFIX = "SIM:";
    private static final String BEHAVIOR_PREFIX = "BHV:";

    /**
     * Mapper：处理用户相似度数据
     * 输入：userId1,userId2\tsimilarityScore
     * 输出：<userId1, SIM:userId2:score> 和 <userId2, SIM:userId1:score>
     */
    public static class SimilarityMapper extends Mapper<LongWritable, Text, LongWritable, Text> {
        
        private LongWritable outKey = new LongWritable();
        private Text outValue = new Text();

        @Override
        protected void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            
            String[] parts = value.toString().split("\t");
            if (parts.length < 2) return;
            
            String[] userPair = parts[0].split(",");
            if (userPair.length < 2) return;
            
            try {
                long userId1 = Long.parseLong(userPair[0]);
                long userId2 = Long.parseLong(userPair[1]);
                double similarity = Double.parseDouble(parts[1]);
                
                // 双向输出：两个用户互为相似用户
                outKey.set(userId1);
                outValue.set(SIMILARITY_PREFIX + userId2 + ":" + similarity);
                context.write(outKey, outValue);
                
                outKey.set(userId2);
                outValue.set(SIMILARITY_PREFIX + userId1 + ":" + similarity);
                context.write(outKey, outValue);
                
            } catch (NumberFormatException e) {
                context.getCounter("OthersAlsoViewed", "SimilarityParseErrors").increment(1);
            }
        }
    }

    /**
     * Mapper：处理用户行为数据
     * 输入：userId\tpropertyId\tbehaviorType\tweight\ttimestamp
     * 输出：<userId, BHV:propertyId:behaviorType:weight>
     */
    public static class BehaviorMapper extends Mapper<LongWritable, Text, LongWritable, Text> {
        
        private LongWritable outKey = new LongWritable();
        private Text outValue = new Text();

        @Override
        protected void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            
            String[] parts = value.toString().split("\t");
            if (parts.length < 4) return;
            
            try {
                long userId = Long.parseLong(parts[0]);
                long propertyId = Long.parseLong(parts[1]);
                int behaviorType = Integer.parseInt(parts[2]);
                double weight = Double.parseDouble(parts[3]);
                
                outKey.set(userId);
                outValue.set(BEHAVIOR_PREFIX + propertyId + ":" + behaviorType + ":" + weight);
                context.write(outKey, outValue);
                
            } catch (NumberFormatException e) {
                context.getCounter("OthersAlsoViewed", "BehaviorParseErrors").increment(1);
            }
        }
    }

    /**
     * Reducer：生成推荐
     * 输入：<userId, [SIM:..., BHV:...]>
     * 输出：<targetUserId, propertyId\tscore\treason>
     */
    public static class RecommendationReducer extends Reducer<LongWritable, Text, Text, Text> {
        
        private Text outKey = new Text();
        private Text outValue = new Text();
        private int topK;

        @Override
        protected void setup(Context context) {
            topK = context.getConfiguration().getInt("recommendation.topk", 10);
        }

        @Override
        protected void reduce(LongWritable key, Iterable<Text> values, Context context) 
                throws IOException, InterruptedException {
            
            long currentUserId = key.get();
            
            // 收集相似用户信息：userId -> similarity
            Map<Long, Double> similarUsers = new HashMap<Long, Double>();
            
            // 收集当前用户的行为（用于排除已浏览房源）
            Set<Long> currentUserProperties = new HashSet<Long>();
            
            for (Text val : values) {
                String data = val.toString();
                
                if (data.startsWith(SIMILARITY_PREFIX)) {
                    // 解析相似用户数据
                    String simData = data.substring(SIMILARITY_PREFIX.length());
                    String[] parts = simData.split(":");
                    if (parts.length >= 2) {
                        long simUserId = Long.parseLong(parts[0]);
                        double similarity = Double.parseDouble(parts[1]);
                        similarUsers.put(simUserId, similarity);
                    }
                } else if (data.startsWith(BEHAVIOR_PREFIX)) {
                    // 记录当前用户浏览过的房源
                    String bhvData = data.substring(BEHAVIOR_PREFIX.length());
                    String[] parts = bhvData.split(":");
                    if (parts.length >= 1) {
                        long propertyId = Long.parseLong(parts[0]);
                        currentUserProperties.add(propertyId);
                    }
                }
            }
            
            // 如果没有相似用户，则跳过
            if (similarUsers.isEmpty()) {
                return;
            }
            
            // 输出相似用户关系（供第二阶段使用）
            for (Map.Entry<Long, Double> entry : similarUsers.entrySet()) {
                outKey.set(String.valueOf(currentUserId));
                outValue.set("SIMILAR_USER:" + entry.getKey() + ":" + entry.getValue());
                context.write(outKey, outValue);
            }
            
            // 输出当前用户已浏览房源（供第二阶段排除）
            for (Long propertyId : currentUserProperties) {
                outKey.set(String.valueOf(currentUserId));
                outValue.set("VIEWED:" + propertyId);
                context.write(outKey, outValue);
            }
        }
    }

    /**
     * 第二阶段Mapper：为相似用户的行为生成推荐候选
     */
    public static class CandidateMapper extends Mapper<LongWritable, Text, Text, Text> {
        
        private Text outKey = new Text();
        private Text outValue = new Text();

        @Override
        protected void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            
            String[] parts = value.toString().split("\t");
            if (parts.length < 2) return;
            
            outKey.set(parts[0]);
            outValue.set(parts[1]);
            context.write(outKey, outValue);
        }
    }

    /**
     * 第二阶段Reducer：汇总推荐并排序
     */
    public static class FinalRecommendationReducer extends Reducer<Text, Text, Text, Text> {
        
        private Text outKey = new Text();
        private Text outValue = new Text();
        private int topK;

        @Override
        protected void setup(Context context) {
            topK = context.getConfiguration().getInt("recommendation.topk", 10);
        }

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) 
                throws IOException, InterruptedException {
            
            String targetUserId = key.toString();
            
            Map<Long, Double> similarUsers = new HashMap<Long, Double>();
            Set<Long> viewedProperties = new HashSet<Long>();
            Map<Long, RecommendationCandidate> candidates = new HashMap<Long, RecommendationCandidate>();
            
            for (Text val : values) {
                String data = val.toString();
                
                if (data.startsWith("SIMILAR_USER:")) {
                    String[] parts = data.substring("SIMILAR_USER:".length()).split(":");
                    if (parts.length >= 2) {
                        similarUsers.put(Long.parseLong(parts[0]), Double.parseDouble(parts[1]));
                    }
                } else if (data.startsWith("VIEWED:")) {
                    viewedProperties.add(Long.parseLong(data.substring("VIEWED:".length())));
                } else if (data.startsWith("CANDIDATE:")) {
                    // 解析候选房源
                    String[] parts = data.substring("CANDIDATE:".length()).split(":");
                    if (parts.length >= 4) {
                        long propertyId = Long.parseLong(parts[0]);
                        long simUserId = Long.parseLong(parts[1]);
                        int behaviorType = Integer.parseInt(parts[2]);
                        double weight = Double.parseDouble(parts[3]);
                        
                        if (!viewedProperties.contains(propertyId)) {
                            Double similarity = similarUsers.get(simUserId);
                            if (similarity == null) similarity = 0.0;
                            double score = calculateRecommendationScore(similarity, behaviorType, weight);
                            
                            RecommendationCandidate candidate = candidates.get(propertyId);
                            if (candidate == null) {
                                candidate = new RecommendationCandidate(propertyId);
                                candidates.put(propertyId, candidate);
                            }
                            candidate.addScore(score, simUserId, behaviorType);
                        }
                    }
                }
            }
            
            // 排序并输出Top-K推荐
            List<RecommendationCandidate> sortedCandidates = new ArrayList<RecommendationCandidate>(candidates.values());
            Collections.sort(sortedCandidates, new Comparator<RecommendationCandidate>() {
                @Override
                public int compare(RecommendationCandidate a, RecommendationCandidate b) {
                    return Double.compare(b.getTotalScore(), a.getTotalScore());
                }
            });
            
            int count = 0;
            for (RecommendationCandidate candidate : sortedCandidates) {
                if (count >= topK) break;
                
                outKey.set(targetUserId);
                outValue.set(candidate.getPropertyId() + "\t" + 
                            String.format("%.4f", candidate.getTotalScore()) + "\t" +
                            candidate.generateReason());
                context.write(outKey, outValue);
                count++;
            }
        }

        private double calculateRecommendationScore(double similarity, int behaviorType, double weight) {
            double behaviorWeight;
            switch (behaviorType) {
                case 1:
                    behaviorWeight = 1.0;  // 浏览
                    break;
                case 2:
                    behaviorWeight = 2.0;  // 收藏
                    break;
                case 3:
                    behaviorWeight = 0.5;  // 搜索
                    break;
                default:
                    behaviorWeight = 1.0;
            }
            return similarity * behaviorWeight * weight;
        }
    }

    /**
     * 推荐候选内部类
     */
    static class RecommendationCandidate {
        private long propertyId;
        private double totalScore;
        private Set<Long> viewerIds;
        private Set<Long> favoriteIds;
        private int viewCount;
        private int favoriteCount;

        public RecommendationCandidate(long propertyId) {
            this.propertyId = propertyId;
            this.totalScore = 0;
            this.viewerIds = new HashSet<Long>();
            this.favoriteIds = new HashSet<Long>();
            this.viewCount = 0;
            this.favoriteCount = 0;
        }

        public void addScore(double score, long userId, int behaviorType) {
            this.totalScore += score;
            if (behaviorType == 1) {
                viewerIds.add(userId);
                viewCount++;
            } else if (behaviorType == 2) {
                favoriteIds.add(userId);
                favoriteCount++;
            }
        }

        public long getPropertyId() { return propertyId; }
        public double getTotalScore() { return totalScore; }

        public String generateReason() {
            List<String> reasons = new ArrayList<String>();
            if (viewerIds.size() > 0) {
                reasons.add(viewerIds.size() + "位相似用户浏览过");
            }
            if (favoriteIds.size() > 0) {
                reasons.add(favoriteIds.size() + "位相似用户收藏过");
            }
            if (reasons.isEmpty()) {
                return "根据相似用户行为推荐";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < reasons.size(); i++) {
                if (i > 0) sb.append("，");
                sb.append(reasons.get(i));
            }
            return sb.toString();
        }
    }

    @Override
    public int run(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: OthersAlsoViewedJob <similarity_input> <behavior_input> <o>");
            return -1;
        }

        Configuration conf = getConf();
        conf.setInt("recommendation.topk", 10);

        // Job 1: 关联用户相似度和行为数据
        Job job1 = Job.getInstance(conf, "Others Also Viewed - Phase 1");
        job1.setJarByClass(OthersAlsoViewedJob.class);
        
        MultipleInputs.addInputPath(job1, new Path(args[0]), TextInputFormat.class, SimilarityMapper.class);
        MultipleInputs.addInputPath(job1, new Path(args[1]), TextInputFormat.class, BehaviorMapper.class);
        
        job1.setReducerClass(RecommendationReducer.class);
        
        job1.setMapOutputKeyClass(LongWritable.class);
        job1.setMapOutputValueClass(Text.class);
        job1.setOutputKeyClass(Text.class);
        job1.setOutputValueClass(Text.class);
        
        Path tempOutput = new Path(args[2] + "_phase1");
        FileOutputFormat.setOutputPath(job1, tempOutput);
        
        if (!job1.waitForCompletion(true)) {
            return 1;
        }

        // Job 2: 汇总并生成最终推荐
        Job job2 = Job.getInstance(conf, "Others Also Viewed - Phase 2");
        job2.setJarByClass(OthersAlsoViewedJob.class);
        
        job2.setMapperClass(CandidateMapper.class);
        job2.setReducerClass(FinalRecommendationReducer.class);
        
        job2.setMapOutputKeyClass(Text.class);
        job2.setMapOutputValueClass(Text.class);
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(Text.class);
        
        org.apache.hadoop.mapreduce.lib.input.FileInputFormat.addInputPath(job2, tempOutput);
        FileOutputFormat.setOutputPath(job2, new Path(args[2]));
        
        boolean success = job2.waitForCompletion(true);
        
        // 清理临时目录
        tempOutput.getFileSystem(conf).delete(tempOutput, true);
        
        return success ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new Configuration(), new OthersAlsoViewedJob(), args);
        System.exit(exitCode);
    }
}