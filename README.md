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

Supported [Nest/Eggs](https://pterodactyl.io/project/terms.html#terminology)
--
- [x] Minecraft
  - [x] Paper/Spigot/Sponge
  - [x] Bungeecord/Velocity
  - [x] Vanilla Minecraft

- [ ] Voiceserver
  - [x] Teamspeak
  - [ ] Mumble (not tested)

- [ ] Source Engines (not tested)
- [ ] Rust (not tested)

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
- snakeyaml (org.yaml:snakeyaml)
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

### 1. Installing Wings
>  TODO

### 2. Configuration
>Once you have installed Wings and the required components, 
>the next step is to create a node on your installed Panel. 
>Go to your Panel administrative view, select Nodes from the sidebar,
>and on the right side click Create New button.
>
>After you have created a node, click on it and there will be a tab called Configuration.
>Copy the code block content, paste it into a new file called **config.yml**  in **/etc/pterodactyl** and save it.
## FAQ
- Soon
 
