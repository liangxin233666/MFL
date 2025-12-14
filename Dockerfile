# 换成了 Java 21 的镜像
FROM eclipse-temurin:21-jre-jammy

# 设置时区为上海 (防止时间不准)
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制 Jar 包
COPY target/MFL-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]