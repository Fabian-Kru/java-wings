# JavaWings
> Wings for Pterodactyl-Panel written in Java and with builtin Kubernetes support.\
> JavaWings replaces the conventional Wings, as a result of the replacement Docker is no longer used internally.\
> Kubernetes is used instead.\
> The aim of JavaWings is to efficiently distribute game servers with short and long lifetimes and to add the possibility of scaling.

> [!NOTE]
> This project is still in development and not ready for production use.

> [!NOTE]
> Discord: get help or ask questions: https://discord.gg/vsMUf8yhp6

## Simple Setup Diagram

![image](https://i.imgur.com/8s2owkd.png)



Prerequisites
--
- Installed https://pterodactyl.io/ Panel
- Kubernetes-Cluster with CIFS Support
- Samba Server or similar (for CIFS)
- Java 17 or higher

optional Requirements
--
- Kubernetes Autoscaler using Hetzner API or AWS ..
- Kubernetes Metrics Server

Feature Status
--
- [x] Built-in SFTP Server 
- [x] Node-Stats
- [ ] Server Mounts
- [x] Create Server
- [x] Delete Server
- [x] Server Console View
- [x] Server Console Commands
- [x] Server Status Change
- [x] Server Statistic (Uptime, CPU, Memory)
- [ ] Server Statistic (Network, Disk)
- [x] Files edit/create-directory/create-file/delete
- [x] File/Folder rename
- [x] File download/upload
- [x] File compress/decompress
- [x] Schedules
- [ ] Users
- [x] Backups (create, restore, lock/unlock)
- [ ] Backups (exclude files)
- [x] Network-Tab
- [x] Settings-Tab
- [ ] use StartUp

Removed Features
--
- Transfer Server (not needed)

Dependencies
--
- bson (org.mongodb:bson)
- java-jwt (com.auth0:java-jwt)
- sshd-sftp (org.apache.sshd:sshd-sftp)
- sshd-core (org.apache.sshd:sshd-core)
- org.json (org.json:json)
- eddsa (net.i2p.crypto:eddsa)
- zip4j (net.lingala.zip4j:zip4j)
- [Mina-sshd](https://github.com/apache/mina-sshd)
- [Kubernetes-Java](https://github.com/kubernetes-client/java)
- [hcloud](https://github.com/hetznercloud/hcloud-cloud-controller-manager) (for Hetzner)

## Known Issues
- None
## Installation
- Soon
## FAQ
- Soon
 
