jvm_app(name='main',
        dependencies = [
            ':main-bin'
        ],
        bundles = [
            bundle(relative_to='config').add(globs('config/*'))
        ]
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