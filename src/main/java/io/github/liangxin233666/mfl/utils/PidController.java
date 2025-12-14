package io.github.liangxin233666.mfl.utils;

public class PidController {
    private double kp; // 比例
    private double ki; // 积分
    private double kd; // 微分

    // 状态变量
    private double previousError = 0;
    private double integral = 0;

    // 输出限制
    private final int minOutput;
    private final int maxOutput;

    public PidController(double kp, double ki, double kd, int minOutput, int maxOutput) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.minOutput = minOutput;
        this.maxOutput = maxOutput;
    }

    /**
     * 计算 PID 输出
     *
     * @param targetValue  目标值（通常为 0）
     * @param currentValue 实际值（当前积压量）
     * @return 建议的总线程数
     */
    public int compute(double targetValue, double currentValue) {
        double error = currentValue - targetValue;

        // 1. 积分计算（含积分分离/抗饱和逻辑）
        // 如果误差很大，或者输出已经达到极限，暂停积分累加，防止"积分饱和"导致系统反应迟钝
        if (Math.abs(error) < 1000) {
            integral += error;
        }

        // 2. 微分计算
        double derivative = error - previousError;

        // 3. 核心公式
        double output = (kp * error) + (ki * integral) + (kd * derivative);

        // 4. 更新状态
        previousError = error;

        // 5. 计算最终结果（基础输出设为 minOutput，即无积压时保持最低配置）
        // 注意：这里我们将 PID 的输出视为"增量"叠加在 minOutput 上
        int finalOutput = (int) (minOutput + output);

        // 6. 钳制输出范围 (Clamping)
        return Math.max(minOutput, Math.min(finalOutput, maxOutput));
    }
}