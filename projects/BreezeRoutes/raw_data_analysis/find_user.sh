# Find all of the traces by a given user
find ./runkeeper-data/boston/breeze_geo/ -type f -exec grep -l "$@" {} +
