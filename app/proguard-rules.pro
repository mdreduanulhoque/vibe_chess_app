# Add project specific ProGuard / R8 rules here.

# Keep core enums used with Enum.valueOf() in bundle args & compact state serialization
-keepclassmembers enum com.reduan.vibechess.core.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep GameViewModel constructor used by SavedStateViewModelFactory
-keep class com.reduan.vibechess.ui.game.GameViewModel {
    public <init>(android.app.Application, androidx.lifecycle.SavedStateHandle);
}

# Keep custom Canvas Views inflated from XML layouts
-keep public class com.reduan.vibechess.ui.game.BoardView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keep public class com.reduan.vibechess.ui.game.WinProbabilityBarView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keep public class com.reduan.vibechess.ui.game.CapturedPiecesTrayView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
