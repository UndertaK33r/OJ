package com.example.sandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import com.example.sandbox.model.ExecuteMessage;
import com.example.sandbox.model.JudgeInfo;
import com.example.sandbox.utils.ProcessUtils;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class JavaDockerCodeSandboxImpl extends JavaCodeSandboxTemplate {

    private static final long TIME_OUT = 20000L;

    //初次启动修改为true
    private static final Boolean FIRST_INIT = true;


        @Override
        public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
            //3.创建容器，上传编译文件
            // 获取默认的 Docker Client
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            DockerClient dockerClient = DockerClientBuilder.getInstance().build();

            // 拉取镜像
            String image = "openjdk:8-alpine";
            if (FIRST_INIT) {
                PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
                PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                    @Override
                    public void onNext(PullResponseItem item) {
                        System.out.println("下载镜像：" + item.getStatus());
                        super.onNext(item);
                    }
                };
                try {
                    pullImageCmd
                            .exec(pullImageResultCallback)
                            .awaitCompletion();
                } catch (InterruptedException e) {
                    System.out.println("下载镜像异常");
                    throw new RuntimeException(e);
                }
            }
            System.out.println("下载完成");

            // 创建容器，最大内存 100MB，最大 CPU 100%
            HostConfig hostConfig = new HostConfig();
            CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
            hostConfig.withMemory(100 * 1024 * 1024L);
            hostConfig.withMemorySwap(0L);
            hostConfig.withCpuCount(1L);
            hostConfig.withSecurityOpts(Arrays.asList("seccomp=unconfined"));
            //容器挂载目录
            hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));

            CreateContainerResponse createContainerResponse = containerCmd
                    .withNetworkDisabled(true)
                    .withHostConfig(hostConfig)
                    .withReadonlyRootfs(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withTty(true)
                    .exec();
            System.out.println(createContainerResponse);
            String containerId = createContainerResponse.getId();


            // 4.启动容器,执行代码
            dockerClient.startContainerCmd(containerId).exec();
            //~$ docker exec cranky_shtern java -cp /app Main 1 3
            //执行命令并获取输出
            List<ExecuteMessage> executeMessageList = new ArrayList<>();
            for (String inputArgs : inputList) {
                StopWatch stopWatch = new StopWatch();

                String[] inputArgsArray = inputArgs.split(" ");
                String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);

                ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                        .withCmd(cmdArray)
                        .withAttachStdout(true)
                        .withAttachStderr(true)
                        .withAttachStdin(true)
                        .exec();

                System.out.println("创建执行命令"+execCreateCmdResponse);


                ExecuteMessage executeMessage = new ExecuteMessage();
                final String[] message = {null};
                final String[] errorMsg = {null};
                long time = 0L;
                final boolean[] timeout = {true};
                String execId = execCreateCmdResponse.getId();
                ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {

                    @Override
                    public void onComplete() {
                        // 如果执行完成，则表示没超时
                        timeout[0] = false;
                        super.onComplete();
                    }

                    @Override
                    public void onNext(Frame item) {
                        StreamType streamType = item.getStreamType();
                        if (StreamType.STDERR.equals(streamType)) {
                            errorMsg[0] = new String(item.getPayload());
                            System.out.println("执行错误输出：" + errorMsg[0]);
                        }else {
                            message[0] = new String(item.getPayload());
                            System.out.println("执行命令输出：" + message[0]);
                        }
                        super.onNext(item);
                    }
                };

                final long[] maxMemory = {0L};

                // 获取占用的内存
                StatsCmd statsCmd = dockerClient.statsCmd(containerId);
                ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {

                    @Override
                    public void onNext(Statistics statistics) {
                        System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                        maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                    }

                    @Override
                    public void close() throws IOException {

                    }

                    @Override
                    public void onStart(Closeable closeable) {

                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
                statsCmd.exec(statisticsResultCallback);
                try {
                    stopWatch.start();
                    dockerClient.execStartCmd(execId)
                            .exec(execStartResultCallback)
                            .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                    stopWatch.stop();
                    time = stopWatch.getLastTaskTimeMillis();
                    statsCmd.close();
                } catch (InterruptedException e) {
                    System.out.println("程序执行异常");
                    throw new RuntimeException(e);
                }
                executeMessage.setMessage(message[0]);
                executeMessage.setErrorMessage(errorMsg[0]);
                executeMessage.setTime(time);
                executeMessage.setMemory(maxMemory[0]);
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            }
            return executeMessageList;
        }



}
