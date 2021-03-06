/**
 * 
 */
package androidx.pluginmgr;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;
import dalvik.system.DexClassLoader;

/**
 * 插件类加载器
 * 
 * @author HouKangxi
 * 
 */
class PluginClassLoader extends DexClassLoader {
	private final String tag;
	private final PlugInfo thisPlugin;
	private final String optimizedDirectory;
	private final String libraryPath;
	/**
	 * Activity 的类加载器
	 */
	private final Map<String, ClassLoader> proxyActivityLoaderMap;

	public PluginClassLoader(String dexPath, String optimizedDir, ClassLoader parent, PlugInfo plugin) {
		super(dexPath, optimizedDir,plugin.getPackageInfo().applicationInfo.nativeLibraryDir,parent);
		thisPlugin = plugin;
		proxyActivityLoaderMap = new HashMap<String, ClassLoader>(plugin.getActivities().size());
		this.libraryPath = plugin.getPackageInfo().applicationInfo.nativeLibraryDir;
		this.optimizedDirectory = optimizedDir;
		tag = "PluginClassLoader( " + plugin.getPackageInfo().packageName + " )";
	}

	Class<?> loadActivityClass(final String actClassName) throws ClassNotFoundException {
		Log.d(tag, "loadActivityClass: " + actClassName);

		File dexSaveDir = ActivityOverider.getPorxyActivityDexPath(thisPlugin, actClassName);
		// 在类加载之前检查创建代理的Activity dex文件，以免调用者忘记生成此文件
		ActivityOverider.createProxyDex(thisPlugin, actClassName, dexSaveDir, true);
		ClassLoader actLoader = proxyActivityLoaderMap.get(actClassName);
		if (actLoader == null) {
			actLoader = new DexClassLoader(dexSaveDir.getAbsolutePath(), optimizedDirectory,libraryPath, this){
				@Override
				protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
					Log.d("PlugActClassLoader("+ actClassName+")", "loadClass: " + name);
					if (ActivityOverider.targetClassName.equals(name)) {
						Class<?> c = findLoadedClass(name);
						if (c == null) {
							Log.d("PlugActClassLoader("+ actClassName+")", "findClass");
							c = findClass(name);
						}
						if (resolve) {
							resolveClass(c);
						}
						return c;
					}
					return super.loadClass(name, resolve);
				}
			};
			proxyActivityLoaderMap.put(actClassName, actLoader);
		}
		return actLoader.loadClass(ActivityOverider.targetClassName);
	}
	
}
