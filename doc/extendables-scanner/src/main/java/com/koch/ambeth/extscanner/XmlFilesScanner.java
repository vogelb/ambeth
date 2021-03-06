package com.koch.ambeth.extscanner;

import java.io.File;
import java.util.SortedMap;

import com.koch.ambeth.ioc.IInitializingBean;
import com.koch.ambeth.ioc.config.Property;
import com.koch.classbrowser.java.OutputUtil;
import com.koch.classbrowser.java.TypeDescription;

public class XmlFilesScanner implements IInitializingBean, IXmlFilesScanner {
	@Property(name = "scan-path")
	protected File scanPath;

	protected SortedMap<String, TypeDescription> javaTypes;

	protected SortedMap<String, TypeDescription> csharpTypes;

	@Override
	public void afterPropertiesSet() throws Throwable {
		File javaFile = new File(scanPath, "export_java.xml");
		File csharpFile = new File(scanPath, "export_csharp.xml");
		if (!javaFile.exists()) {
			throw new IllegalArgumentException("Java XML file not found: " + javaFile.getPath());
		}
		if (!csharpFile.exists()) {
			throw new IllegalArgumentException("Csharp XML file not found: " + csharpFile.getPath());
		}
		javaTypes = OutputUtil.importFromFile(javaFile.getPath());
		csharpTypes = OutputUtil.importFromFile(csharpFile.getPath());
	}

	@Override
	public SortedMap<String, TypeDescription> getCsharpTypes() {
		return csharpTypes;
	}

	@Override
	public SortedMap<String, TypeDescription> getJavaTypes() {
		return javaTypes;
	}
}
