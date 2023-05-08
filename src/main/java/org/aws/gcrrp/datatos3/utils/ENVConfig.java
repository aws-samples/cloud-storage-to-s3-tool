package org.aws.gcrrp.datatos3.utils;

import software.amazon.awssdk.regions.Region;

public class ENVConfig {

    public static String awsRegion = System.getProperty("aws_region");
    public static String amiID = "ami-03190fe20ef6b1419";
    public static String transferS3LogPath = "s3:/transfer-log-rclone/";
    public static String rcloneCommand = "rclone copy -v {SourceLocation} {DestinationLocation} ";
    public static String emailFrom = "xxxx@163.com";
    public static String emailTo = "xxxxx@amazon.com";
    public static String sqsQueueUrl = "";
    public static boolean isCompleted = false;
    public static boolean sendMessage = true;


    public static Region getRegion() {
        return Region.of(awsRegion);
    } // todo update to return System.getProperty("aws_region")

//    public static void main(String[] args) {
//        System.out.println(ENVConfig.getRegion().id());
//        System.out.println(Region.AP_SOUTHEAST_1.isGlobalRegion());
//    }
}
