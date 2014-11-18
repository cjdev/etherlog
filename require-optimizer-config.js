({
    mainConfigFile:"${basedir}/src/main/resources/content/require-config.js",
    appDir: "${basedir}/src/main/resources/content",
    baseUrl: "scripts/",
    dir: "${basedir}/target/classes/content",
    optimize:"uglify",
    keepBuildDir:true,
    modules: [
        {
            name: "backlog"
        },
        {
            name:"frontPage"
        }
    ]
})