# Shizuku uses reflection to construct the local user service.
-keep class com.xiaojinzi.tally.module.qianji.authorization.AccessibilityGrantService {
    public <init>(android.content.Context);
    public *;
}
