#!/bin/sh
set -eu

cd "$(dirname "$0")"
./restart_bluestacks_instance.sh Tiramisu64_15 127.0.0.1:5705
./restart_bluestacks_instance.sh Tiramisu64_17 127.0.0.1:5725
./restart_bluestacks_instance.sh Tiramisu64_18 127.0.0.1:5735
