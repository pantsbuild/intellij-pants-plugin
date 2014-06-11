jvm_app(name='name',
        dependencies = [
            ':main-bin'
        ],
        bundles = [
            bundle(relative_to='config').add(globs('config/*'))
        ]
)
bad_statement(not_name='this_is_not_a_target',
              dependencies = [
                  pants('not/a/file/path/name=confused')
              ],
              source = name = 'bad_target',
)
# The binary, the "runnable" part:

jvm_binary(name = 'main-bin',
           dependencies = [
               pants('src/java/com/pants/examples/hello/greet'),
               ],
           resources=[
               pants('src/resources/com/pants/example/hello'),
               ],
           source = 'HelloMain.java',
           main = 'com.pants.examples.hello.main.HelloMain',
           )