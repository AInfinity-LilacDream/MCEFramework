# MusicDodge 粒子优化系统

## 概述

这个系统通过Plugin Message Channel将粒子渲染工作转移到客户端，显著减少服务端的网络发包量，解决MusicDodge游戏中因大量粒子发包导致的网络拥塞和丢包问题。

## 核心组件

### 1. AttackDataEncoder.java
- 将攻击数据编码为字符串格式
- 支持激光、正方形环、旋转攻击等基本攻击类型
- 提供编码/解码功能

### 2. AttackDataManager.java
- 全局管理所有活跃攻击
- 每tick收集攻击状态并发送给客户端
- 提供TrackableAttack接口和包装器类

### 3. DustParticleInterceptor.java
- 使用ProtocolLib拦截所有DUST粒子包
- 防止原始粒子包发送到客户端
- 可以动态启用/禁用

### 4. ClientParticleRenderer.java
- 客户端粒子渲染逻辑示例
- 展示如何解析攻击数据并渲染粒子
- 包含客户端Mod集成示例代码

### 5. ParticleOptimizationIntegration.java
- 系统集成示例
- 展示如何将现有攻击与新系统集成
- 处理复合攻击的拆解逻辑

## 数据格式

### 编码格式
```
攻击类型|参数|颜色|剩余时间#攻击类型|参数|颜色|剩余时间
```

### 示例
```
LASER|10.0,60.0,20.0,30.0,60.0,20.0|RED|120#SPIN|14.0,60.0,25.0,5,45.0,100|GRAY|240
```

### 攻击类型参数

#### LASER (激光)
```
x1,y1,z1,x2,y2,z2
```

#### SQUARE_RING (正方形环)
```
centerX,centerY,centerZ,innerRadius,outerRadius
```

#### SPIN (旋转攻击)
```
centerX,centerY,centerZ,rayCount,angleOffset,maxDistance
```

#### CIRCLE (圆形攻击)
```
centerX,centerY,centerZ,radius
```

#### WALL (墙攻击)
```
direction,position
```

## 使用方法

### 1. 启用系统

```java
// 在MusicDodge游戏开始时
AttackDataManager manager = AttackDataManager.getInstance(plugin);
DustParticleInterceptor interceptor = DustParticleInterceptor.create(plugin);

// 启用粒子拦截和数据发送
interceptor.enable();
manager.start();
```

### 2. 注册攻击

```java
// 创建TrackableAttack包装器
AttackDataManager.TrackableSpinAttack trackableAttack = 
    new AttackDataManager.TrackableSpinAttack(originalAttack, center, rayCount, 
                                            rotationSpeed, maxDistance, alertBeats, attackBeats, bpm);

// 注册到管理器
manager.registerAttack(trackableAttack);

// 启动原始攻击（用于伤害检测）
originalAttack.toggle();
```

### 3. 复合攻击处理

复合攻击（如SideAttack、BarAttack）需要拆解为基本攻击：

```java
// SideAttack -> 多个SquareRingAttack
// BarAttack -> 多个LaserAttack  
// RandomAttack -> 多个基本攻击
// RandomLaserAttack -> 多个LaserAttack
```

### 4. 停止系统

```java
// 在游戏结束时
interceptor.disable();
manager.stop();
```

## 客户端实现

### Plugin Message监听

客户端Mod需要监听 `mce:musicdodge` 频道：

```java
@SubscribeEvent
public void onClientCustomPayload(ClientCustomPayloadEvent event) {
    if ("mce:musicdodge".equals(event.getPayload().location().toString())) {
        String data = new String(event.getPayload().data().array(), "UTF-8");
        handleAttackData(data);
    }
}
```

### 粒子渲染

客户端根据攻击数据计算粒子位置并渲染：

```java
private void handleAttackData(String encodedData) {
    List<AttackDataEncoder.AttackData> attacks = AttackDataEncoder.decodeAttacks(encodedData);
    
    for (AttackDataEncoder.AttackData attack : attacks) {
        renderClientParticles(attack, mc.level);
    }
}
```

## 性能优化

### 网络流量减少
- 原来：每个粒子一个包（大量小包）
- 现在：每tick一个编码字符串（单个较大包）
- 估计减少 80-90% 的网络包数量

### 服务端性能
- 减少粒子包生成和发送的CPU开销
- 减少网络带宽占用
- 更稳定的网络连接

### 客户端性能
- 客户端可以根据性能动态调整粒子密度
- 更流畅的粒子动画
- 减少网络延迟影响

## 兼容性

### 向后兼容
- 如果客户端没有Mod，系统自动回退到原始粒子发送
- 可以根据玩家是否有Mod选择性启用优化

### 错误处理
- 粒子拦截器有异常处理，避免影响其他功能
- 攻击数据编码/解码有容错机制
- 单个攻击出错不影响其他攻击

## 集成到现有系统

现有的MusicDodge类已经集成了这个系统：

1. 在构造函数中初始化组件
2. 在onCycleStart()中启用系统
3. 在onEnd()和stop()中禁用系统
4. 保持原有攻击逻辑不变（用于伤害检测）

## 扩展性

### 新攻击类型
添加新攻击类型只需要：
1. 在AttackType枚举中添加新类型
2. 在AttackDataEncoder中添加创建和解析方法
3. 在ClientParticleRenderer中添加渲染逻辑
4. 创建对应的TrackableAttack包装器

### 其他游戏集成
这个系统可以轻松扩展到其他使用粒子效果的游戏：
1. 修改Plugin Message频道名称
2. 适配游戏特定的攻击类型
3. 调整粒子拦截逻辑

## 调试和监控

### 日志输出
- 拦截的粒子包数量
- 发送的攻击数据大小
- 客户端连接状态

### 性能指标
- 网络包数量对比
- 带宽使用量
- 客户端渲染性能

## 注意事项

1. 需要客户端Mod配合才能获得最佳效果
2. 复合攻击需要正确拆解，确保时序准确
3. 粒子拦截器可能影响其他插件的粒子效果（可配置）
4. 客户端渲染逻辑需要与服务端保持同步

## 未来改进

1. 支持更多粒子类型（不只是DUST）
2. 动态压缩算法优化数据大小
3. 客户端预测和插值算法
4. 支持自定义粒子效果
5. 可视化调试工具