#!/bin/sh
set -eu

cd "$(dirname "$0")"
./restart_bluestacks_instance.sh Tiramisu64 127.0.0.1:5555
./restart_bluestacks_instance.sh Tiramisu64_14 127.0.0.1:5695
./restart_bluestacks_instance.sh Tiramisu64_15 127.0.0.1:5705
