jar_library(
    name='bin',
    dependencies=[
        'foo/:b<caret>'
    ]
)