FROM ubuntu:latest
LABEL authors="fabiankru.de"

ENTRYPOINT ["top", "-b"]