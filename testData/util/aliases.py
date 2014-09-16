def build_file_aliases():
    return BuildFileAliases.create(
        targets={
            'annotation_processor': AnnotationProcessor,
            'benchmark': Benchmark,
            'credentials': Credentials,
            'jar_library': JarLibrary,
            'java_agent': JavaAgent,
            'java_library': JavaLibrary,
            'java_tests': JavaTests,
            'junit_tests': JavaTests,
            'jvm_app': JvmApp,
            'jvm_binary': JvmBinary,
            'repo': Repository,
            'scala_library': ScalaLibrary,
            'scala_specs': ScalaTests,
            'scala_tests': ScalaTests,
            'scalac_plugin': ScalacPlugin,
            },
        objects={
            'artifact': Artifact,
            'Duplicate': Duplicate,
            'exclude': Exclude,
            'jar': JarDependency,
            'jar_rules': JarRules,
            'Skip': Skip,
            },
        context_aware_object_factories={
            'buildfile_path': BuildFilePath,
            'globs': Globs,
            'rglobs': RGlobs,
            'source_root': SourceRoot,
            'zglobs': ZGlobs,
            }
    )
