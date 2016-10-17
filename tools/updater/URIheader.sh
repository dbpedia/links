#!/bin/bash
curl -I "$1" 2>/dev/null | head -n 1 | cut -d$' ' -f2
