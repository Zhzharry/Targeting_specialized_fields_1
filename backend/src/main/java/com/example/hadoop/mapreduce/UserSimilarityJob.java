package com.example.hadoop.mapreduce;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.*;

/**
 * 用户相似度计算MapReduce作业 (Java 8 兼容版本)
 * 
 * 输入格式：userId\tpropertyId\tbehaviorType\tweight\ttimestamp
 * 输出格式：userId1,userId2\tsimilarityScore
 * 
 * 位置：backend/src/main/java/com/example/hadoop/mapreduce/UserSimilarityJob.java
 */
public class UserSimilarityJob extends Configured implements Tool {

    /**
     * Mapper阶段1：将用户行为按房源ID分组
     * 输入：用户行为记录
     * 输出：<propertyId, userId:weight>
     */
    public static class PropertyUserMapper extends Mapper<LongWritable, Text, LongWritable, Text> {
        
        private LongWritable outKey = new LongWritable();
        private Text outValue = new Text();

        @Override
        protected void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            
            String line = value.toString().trim();
            if (line.isEmpty()) return;
            
            String[] parts = line.split("\t");
            if (parts.length < 4) return;
            
            try {
                long userId = Long.parseLong(parts[0]);
                long propertyId = Long.parseLong(parts[1]);
                int behaviorType = Integer.parseInt(parts[2]);
                double weight = Double.parseDouble(parts[3]);
                
                // 根据行为类型调整权重
                double adjustedWeight = calculateAdjustedWeight(behaviorType, weight);
                
                outKey.set(propertyId);
                outValue.set(userId + ":" + adjustedWeight);
                context.write(outKey, outValue);
                
            } catch (NumberFormatException e) {
                context.getCounter("UserSimilarity", "ParseErrors").increment(1);
            }
        }

        private double calculateAdjustedWeight(int behaviorType, double baseWeight) {
            // 浏览=1.0, 收藏=2.0, 搜索=0.5
            double typeMultiplier;
            switch (behaviorType) {
                case 1:
                    typeMultiplier = 1.0;  // 浏览
                    break;
                case 2:
                    typeMultiplier = 2.0;  // 收藏
                    break;
                case 3:
                    typeMultiplier = 0.5;  // 搜索
                    break;
                default:
                    typeMultiplier = 1.0;
            }
            return baseWeight * typeMultiplier;
        }
    }

    /**
     * Reducer阶段1：计算同一房源下用户对的共现
     * 输入：<propertyId, [userId1:weight1, userId2:weight2, ...]>
     * 输出：<userId1,userId2, coOccurrenceScore>
     */
    public static class UserPairReducer extends Reducer<LongWritable, Text, Text, DoubleWritable> {
        
        private Text outKey = new Text();
        private DoubleWritable outValue = new DoubleWritable();

        @Override
        protected void reduce(LongWritable key, Iterable<Text> values, Context context) 
                throws IOException, InterruptedException {
            
            // 收集该房源下所有用户及其权重
            Map<Long, Double> userWeights = new HashMap<Long, Double>();
            
            for (Text val : values) {
                String[] parts = val.toString().split(":");
                if (parts.length == 2) {
                    try {
                        long userId = Long.parseLong(parts[0]);
                        double weight = Double.parseDouble(parts[1]);
                        Double existing = userWeights.get(userId);
                        if (existing != null) {
                            userWeights.put(userId, existing + weight);
                        } else {
                            userWeights.put(userId, weight);
                        }
                    } catch (NumberFormatException e) {
                        // 忽略解析错误
                    }
                }
            }
            
            // 生成用户对及其共现分数
            List<Long> userIds = new ArrayList<Long>(userWeights.keySet());
            
            for (int i = 0; i < userIds.size(); i++) {
                for (int j = i + 1; j < userIds.size(); j++) {
                    long user1 = Math.min(userIds.get(i), userIds.get(j));
                    long user2 = Math.max(userIds.get(i), userIds.get(j));
                    
                    // 共现分数 = 两个用户权重的乘积
                    double score = userWeights.get(userIds.get(i)) * userWeights.get(userIds.get(j));
                    
                    outKey.set(user1 + "," + user2);
                    outValue.set(score);
                    context.write(outKey, outValue);
                }
            }
        }
    }

    /**
     * Mapper阶段2：直接传递用户对数据
     */
    public static class SimilarityMapper extends Mapper<LongWritable, Text, Text, DoubleWritable> {
        
        private Text outKey = new Text();
        private DoubleWritable outValue = new DoubleWritable();

        @Override
        protected void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            
            String[] parts = value.toString().split("\t");
            if (parts.length >= 2) {
                outKey.set(parts[0]);
                outValue.set(Double.parseDouble(parts[1]));
                context.write(outKey, outValue);
            }
        }
    }

    /**
     * Reducer阶段2：汇总用户相似度
     */
    public static class SimilaritySumReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {
        
        private DoubleWritable outValue = new DoubleWritable();
        private double minSimilarity;

        @Override
        protected void setup(Context context) {
            minSimilarity = context.getConfiguration().getDouble("similarity.min.threshold", 0.1);
        }

        @Override
        protected void reduce(Text key, Iterable<DoubleWritable> values, Context context) 
                throws IOException, InterruptedException {
            
            double totalScore = 0.0;
            int count = 0;
            
            for (DoubleWritable val : values) {
                totalScore += val.get();
                count++;
            }
            
            // 归一化相似度分数（使用对数缩放）
            double normalizedScore = Math.min(1.0, Math.log1p(totalScore) / 10.0);
            
            // 只输出高于阈值的相似度
            if (normalizedScore >= minSimilarity) {
                outValue.set(normalizedScore);
                context.write(key, outValue);
            }
        }
    }

    @Override
    public int run(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: UserSimilarityJob <input> <output>");
            return -1;
        }

        Configuration conf = getConf();
        
        // Job 1: 计算用户对共现
        Job job1 = Job.getInstance(conf, "User Co-occurrence");
        job1.setJarByClass(UserSimilarityJob.class);
        
        job1.setMapperClass(PropertyUserMapper.class);
        job1.setReducerClass(UserPairReducer.class);
        
        job1.setMapOutputKeyClass(LongWritable.class);
        job1.setMapOutputValueClass(Text.class);
        job1.setOutputKeyClass(Text.class);
        job1.setOutputValueClass(DoubleWritable.class);
        
        FileInputFormat.addInputPath(job1, new Path(args[0]));
        Path tempOutput = new Path(args[1] + "_temp");
        FileOutputFormat.setOutputPath(job1, tempOutput);
        
        if (!job1.waitForCompletion(true)) {
            return 1;
        }

        // Job 2: 汇总相似度
        Job job2 = Job.getInstance(conf, "User Similarity Sum");
        job2.setJarByClass(UserSimilarityJob.class);
        
        job2.setMapperClass(SimilarityMapper.class);
        job2.setReducerClass(SimilaritySumReducer.class);
        
        job2.setMapOutputKeyClass(Text.class);
        job2.setMapOutputValueClass(DoubleWritable.class);
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(DoubleWritable.class);
        
        FileInputFormat.addInputPath(job2, tempOutput);
        FileOutputFormat.setOutputPath(job2, new Path(args[1]));
        
        boolean success = job2.waitForCompletion(true);
        
        // 清理临时目录
        tempOutput.getFileSystem(conf).delete(tempOutput, true);
        
        return success ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new Configuration(), new UserSimilarityJob(), args);
        System.exit(exitCode);
    }
}