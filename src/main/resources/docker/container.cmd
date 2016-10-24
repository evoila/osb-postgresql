export REPOSITORY_POSTGRES=$repo_service &&
export REPOSITORY_MAIN=$repos_main &&
apt-get update &&
apt-get install -y wget &&
wget $repo_service/postgres-template.sh && 
chmod +x postgres-template.sh  &&
./postgres-template.sh -d $database_name -u $database_user -p $database_password -e docker &&
top
