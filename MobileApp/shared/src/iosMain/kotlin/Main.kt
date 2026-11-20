import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.janncarlo.kronosim.root.App
import com.janncarlo.kronosim.util.LocalNativeViewFactory
import com.janncarlo.kronosim.util.NativeViewFactory
import com.janncarlo.kronosim.util.SwiftLibDependencyFactory
import com.janncarlo.kronosim.util.swiftLibDependenciesModule
import org.koin.core.KoinApplication
import platform.UIKit.UIViewController

fun MainViewController(nativeViewFactory: NativeViewFactory): UIViewController = ComposeUIViewController {
    CompositionLocalProvider(LocalNativeViewFactory provides nativeViewFactory) {
        App()
    }
}

// This is called on application started on Swift side
fun KoinApplication.provideSwiftLibDependencyFactory(factory: SwiftLibDependencyFactory) = run { modules(swiftLibDependenciesModule(factory)) }
