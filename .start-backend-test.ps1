$env:MYSQL_PORT='3306'
$env:DB_HOST='127.0.0.1'
$env:DB_USERNAME='worksite'
$env:DB_PASSWORD='worksite'
$env:MINIO_API_PORT='9010'
$env:REDIS_PORT='6389'
$env:AI_DATA_SOURCE_PASSWORD_KEY='0123456789abcdef'
mvn spring-boot:run
