$cp = "target\classes"
$repo = "$HOME\.m2\repository"
$libs = @(
    "org\hibernate\orm\hibernate-core\6.6.4.Final\hibernate-core-6.6.4.Final.jar",
    "org\hibernate\orm\hibernate-community-dialects\6.6.4.Final\hibernate-community-dialects-6.6.4.Final.jar",
    "org\xerial\sqlite-jdbc\3.46.1.3\sqlite-jdbc-3.46.1.3.jar",
    "org\slf4j\slf4j-simple\2.0.16\slf4j-simple-2.0.16.jar",
    "org\slf4j\slf4j-api\2.0.16\slf4j-api-2.0.16.jar",
    "jakarta\persistence\jakarta.persistence-api\3.1.0\jakarta.persistence-api-3.1.0.jar",
    "jakarta\transaction\jakarta.transaction-api\2.0.1\jakarta.transaction-api-2.0.1.jar",
    "org\jboss\logging\jboss-logging\3.5.0.Final\jboss-logging-3.5.0.Final.jar",
    "org\hibernate\common\hibernate-commons-annotations\7.0.3.Final\hibernate-commons-annotations-7.0.3.Final.jar",
    "net\bytebuddy\byte-buddy\1.14.18\byte-buddy-1.14.18.jar",
    "com\fasterxml\classmate\1.5.1\classmate-1.5.1.jar",
    "jakarta\inject\jakarta.inject-api\2.0.1\jakarta.inject-api-2.0.1.jar",
    "jakarta\xml\bind\jakarta.xml.bind-api\4.0.0\jakarta.xml.bind-api-4.0.0.jar",
    "jakarta\activation\jakarta.activation-api\2.1.0\jakarta.activation-api-2.1.0.jar",
    "org\antlr\antlr4-runtime\4.13.0\antlr4-runtime-4.13.0.jar",
    "io\smallrye\jandex\3.2.0\jandex-3.2.0.jar",
    "com\microsoft\playwright\playwright\1.49.0\playwright-1.49.0.jar",
    "com\microsoft\playwright\driver\1.49.0\driver-1.49.0.jar",
    "com\microsoft\playwright\driver-bundle\1.49.0\driver-bundle-1.49.0.jar",
    "com\google\code\gson\gson\2.11.0\gson-2.11.0.jar"
)

foreach ($lib in $libs) {
    $path = Join-Path $repo $lib
    if (Test-Path $path) {
        $cp += ";$path"
    } else {
        Write-Host "Warning: Missing $path"
    }
}

java --enable-preview -cp $cp Main
