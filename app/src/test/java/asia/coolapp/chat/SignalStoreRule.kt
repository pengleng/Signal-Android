package asia.coolapp.chat

import androidx.test.core.app.ApplicationProvider
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.dependencies.MockApplicationDependencyProvider
import asia.coolapp.chat.keyvalue.KeyValueDataSet
import asia.coolapp.chat.keyvalue.KeyValueStore
import asia.coolapp.chat.keyvalue.MockKeyValuePersistentStorage
import asia.coolapp.chat.keyvalue.SignalStore

/**
 * Rule to setup [SignalStore] with a mock [KeyValueDataSet]. Must be used with Roboelectric.
 *
 * Can provide [defaultValues] to set the same values before each test and use [dataSet] directly to add any
 * test specific values.
 *
 * The [dataSet] is reset at the beginning of each test to an empty state.
 */
class SignalStoreRule @JvmOverloads constructor(private val defaultValues: KeyValueDataSet.() -> Unit = {}) : TestRule {
  var dataSet = KeyValueDataSet()
    private set

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      @Throws(Throwable::class)
      override fun evaluate() {
        if (!ApplicationDependencies.isInitialized()) {
          ApplicationDependencies.init(ApplicationProvider.getApplicationContext(), MockApplicationDependencyProvider())
        }

        dataSet = KeyValueDataSet()
        SignalStore.inject(KeyValueStore(MockKeyValuePersistentStorage.withDataSet(dataSet)))
        defaultValues.invoke(dataSet)

        base.evaluate()
      }
    }
  }
}
