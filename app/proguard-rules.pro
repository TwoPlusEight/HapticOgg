# slim 构建类型（R8 + 资源压缩）使用的规则。
# FFmpegKit 的 AAR 自带 consumer 规则，会保留 JNI 调用的 native 方法，
# 所以这里默认不需要额外配置。仅在 R8 报缺失类或运行期出现
# ClassNotFoundException / NoSuchMethodError 时，才往下面追加 keep 规则。
#
# 例如需要保留某个被反射调用的类时：
# -keep class com.example.hapticogg.SomeReflectiveClass { *; }
#
# 需要 R8 忽略缺失的可选依赖时：
# -dontwarn com.example.optional.**
