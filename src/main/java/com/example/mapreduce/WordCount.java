package com.example.mapreduce;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class WordCount {
    
    public static void main(String[] args) throws Exception {
        int result = runWordCount(args);
        System.exit(result);
    }
    
    /**
     * 运行WordCount作业的静态方法，供Spring Boot应用调用
     * @param args 输入路径和输出路径
     * @return 0表示成功，非0表示失败
     */
    public static int runWordCount(String[] args) {
        try {
            if (args.length != 2) {
                System.err.println("Usage: WordCount <input path> <output path>");
                return -1;
            }
            
            String inputPath = args[0];
            String outputPath = args[1];
            
            System.out.println("开始执行WordCount作业...");
            System.out.println("输入路径: " + inputPath);
            System.out.println("输出路径: " + outputPath);
            
            Configuration conf = new Configuration();
            // 设置Hadoop配置
            conf.set("fs.defaultFS", "hdfs://localhost:9000");
            conf.set("HADOOP_USER_NAME", "root");
            conf.set("mapreduce.framework.name", "local");
            
            Job job = Job.getInstance(conf, "word count");
            job.setJarByClass(WordCount.class);
            job.setMapperClass(WordCountMapper.class);
            job.setCombinerClass(WordCountReducer.class);
            job.setReducerClass(WordCountReducer.class);
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(IntWritable.class);
            
            FileInputFormat.addInputPath(job, new Path(inputPath));
            FileOutputFormat.setOutputPath(job, new Path(outputPath));
            
            boolean success = job.waitForCompletion(true);
            if (success) {
                System.out.println("WordCount作业执行成功！");
                System.out.println("结果已保存到: " + outputPath);
            } else {
                System.out.println("WordCount作业执行失败！");
            }
            
            return success ? 0 : 1;
            
        } catch (Exception e) {
            System.err.println("执行WordCount作业时发生错误: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
}
