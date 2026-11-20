#!/usr/bin/env bash
# Uso (desde la RAÍZ de tu carpeta kronosim-kmp, la original, no el ZIP):
#   ./commit_my_work.sh "Tu Nombre" "tu@email.com" "https://github.com/TU_USUARIO/kronosim-kmp.git"
#
# Qué hace:
#   1. Conserva el historial de la plantilla (es honesto y las reglas la promueven).
#   2. Separa tu trabajo en commits propios, con la fecha real de hoy.
#   3. Añade tu nombre al LICENSE (sin quitar los avisos originales, que MIT exige).
#   4. Deja la plantilla como remoto "upstream" y tu repo como "origin".
#   5. NO hace push: te muestra el comando al final.
set -euo pipefail

NAME="${1:?Falta el nombre}"
EMAIL="${2:?Falta el email}"
REMOTE_URL="${3:?Falta la URL de tu repo vacío en GitHub}"

[ -d .git ] && [ -d MobileApp ] || { echo "Ejecuta esto en la raíz de kronosim-kmp"; exit 1; }

git config user.name "$NAME"
git config user.email "$EMAIL"
git checkout -q main

S=MobileApp/shared/src/commonMain/kotlin/com/janncarlo/kronosim
SIM=(
  "$S/domain/engine"
  "$S/domain/model/ClusterModels.kt"
  "$S/presentation/screens/simulator"
  "$S/data/source/local/entity/SavedTopologyEntity.kt"
  "$S/data/source/local/dao/SavedTopologyDao.kt"
  "MobileApp/shared/src/commonTest/kotlin/com/janncarlo/kronosim/domain"
)

# Commit 1: renombrado de la plantilla (todo menos el simulador)
git add -A
git reset -q -- "${SIM[@]}"
git commit -q -m "Rename starter kit to KronoSim (package, app id, config)"

# Commit 2: lo que realmente es tuyo
git add "${SIM[@]}"
git commit -q -m "Add Raft consensus engine, simulator screen and saved topologies"

# Commit 3: tu nombre en el LICENSE (se conservan los copyright originales)
if ! grep -q "$NAME" LICENSE; then
  sed -i "/Measify Kft/a Copyright (c) 2026 $NAME" LICENSE
  git add LICENSE
  git commit -q -m "Add contest author to LICENSE"
fi

# Remotos
if git remote | grep -qx origin; then git remote rename origin upstream; fi
git remote add origin "$REMOTE_URL"

echo
git log --oneline -5
echo
echo "Listo. Para publicar (el repo de GitHub debe estar vacío y ser PÚBLICO):"
echo "  git push -u origin main"
