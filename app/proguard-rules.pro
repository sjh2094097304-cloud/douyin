# ========== Xposed 核心入口 ==========
-keep class com.autumn.douyin.liquidglass.hook.** { *; }

# ========== 后台核心组件 ==========
-keep class com.autumn.douyin.liquidglass.root.CompositeFrameDaemon { *; }
-keep class com.autumn.douyin.liquidglass.capture.ScreenCaptureService { *; }

# ========== 配置与存储 ==========
# 自定义 ContentProvider，Manifest 注册类防混淆
-keep class com.autumn.douyin.liquidglass.settings.ModuleSettingsProvider { *; }
# 配置常量类，避免字段重命名导致读取失效
-keep class com.autumn.douyin.liquidglass.settings.ModuleSettingsStore { *; }

# ========== 注解保留（R8 全量优化必加） ==========
-keepattributes *Annotation*

-keepclasseswithmembernames class * {
    native <methods>;
}