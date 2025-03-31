package com.example.sandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DockerClientBuilder;

public class DockerDemo {
    public static void main(String[] args) throws InterruptedException {
        //获取默认的docker客户端
        DockerClient dockerClient =  DockerClientBuilder.getInstance().build();
//        PingCmd pingCmd = dockerClient.pingCmd();
//        pingCmd.exec();
        String imageName = "nginx:latest";
        PullImageCmd pullImagecmd = dockerClient.pullImageCmd(imageName);
        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback(){
          @Override
          public void onNext(PullResponseItem item) {
            System.out.println(item.getStatus());
            super.onNext(item);
          }
        };
        pullImagecmd.exec(pullImageResultCallback)
                .awaitCompletion();
        System.out.println("拉取镜像完成");
    }
}
