package de.fabiankru.javawings.model.jwt;

import lombok.Getter;

import static de.fabiankru.javawings.JavaWings.logger;

@Getter
public enum JWTPermissions {


    ALL("*"),
    PermissionReadDatabase("database.read"),
    PermissionReadStartup("startup.read"),
    PermissionReadAllocation("allocation.read"),
    PermissionReadSchedule("schedule.read"),
    PermissionReadActivity("activity.read"),
    PermissionConnect("websocket.connect"),
    PermissionSendCommand("control.console"),
    PermissionSendPowerStart("control.start"),
    PermissionSendPowerStop("control.stop"),
    PermissionSendPowerRestart("control.restart"),
    PermissionReceiveErrors("admin.websocket.errors"),
    PermissionReceiveInstall("admin.websocket.install"),
    PermissionReceiveTransfer("admin.websocket.transfer"),
    PermissionReceiveBackups("backup.read");

    private final String permission;

    JWTPermissions(String s) {
        this.permission = s;
    }

    public static JWTPermissions getByPermission(String permission) {
        for(JWTPermissions p : JWTPermissions.values()) {
            if(p.getPermission().equals(permission)) {
                return p;
            }
        }
        logger.info("Permission not found: " + permission);
        return null;
    }
}
