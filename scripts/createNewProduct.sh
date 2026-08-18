#! /bin/sh
# Scaffold a new vendor package from templates/.
# Portable: sed -i differs between GNU and BSD/macOS, so we use -i.bak + rm.

if [ $# -ne 1 ]; then
    echo "Usage: $0 <folder_path>   (e.g. $0 package/acme)"
    exit 1
fi

FOLDER_PATH=$1;
FOLDER_NAME=$(basename "$FOLDER_PATH");
BASE_DIR=$(dirname "$0");

mkdir -p "$FOLDER_PATH"

# templates/. (not templates/*) so dotfiles like the REQUIRED .npmrc copy too
cp -r "$BASE_DIR"/../templates/. "$BASE_DIR/../$FOLDER_PATH"

sed -i.bak "s/{code}/$FOLDER_NAME/g" "$BASE_DIR/../$FOLDER_PATH/package.json"

UUID=$(uuidgen | tr '[:upper:]' '[:lower:]')
sed -i.bak "s/{id}/$UUID/g" "$BASE_DIR/../$FOLDER_PATH/index.yml"
sed -i.bak "s/{code}/$FOLDER_NAME/g" "$BASE_DIR/../$FOLDER_PATH/index.yml"
rm -f "$BASE_DIR/../$FOLDER_PATH/package.json.bak" "$BASE_DIR/../$FOLDER_PATH/index.yml.bak"

echo "Scaffolded $FOLDER_PATH (code: $FOLDER_NAME, id: $UUID)"
echo "Next: fill in the {name}/{description}/{url} placeholders in index.yml,"
echo "      add the vendor logo, and create build.gradle.kts:"
echo "      echo 'plugins { id(\"zb.content\") }' > $FOLDER_PATH/build.gradle.kts"
