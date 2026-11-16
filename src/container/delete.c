/*
 * Droidspaces v7 - High-performance Container Runtime
 *
 * Copyright (C) 2026 ravindu644 <droidcasts@protonmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#include "droidspaces.h"

#include "internal.h"

int delete_container(struct ds_config *cfg) {
  if (!cfg || cfg->container_name[0] == '\0') {
    ds_error("Container name is required for delete.");
    return -1;
  }

  int lock_fd = lifecycle_lock_or_complain(cfg, "delete");
  if (lock_fd < 0)
    return -1;

  int ret = -1;
  pid_t pid = 0;
  if (is_container_running(cfg, &pid)) {
    ds_error("Container '%s' is running (PID %d). Stop it before deleting.",
             cfg->container_name, (int)pid);
    goto out;
  }

  char safe_name[256];
  sanitize_container_name(cfg->container_name, safe_name, sizeof(safe_name));

  char container_dir[PATH_MAX];
  int r = snprintf(container_dir, sizeof(container_dir), "%s/%s/%s",
                   get_workspace_dir(), DS_CONTAINERS_DIR, safe_name);
  if (r <= 0 || (size_t)r >= sizeof(container_dir)) {
    ds_error("Container path is too long for '%s'", cfg->container_name);
    goto out;
  }

  if (access(container_dir, F_OK) != 0) {
    ds_error("Container '%s' not found.", cfg->container_name);
    goto out;
  }

  /* A stale pidfile can be left behind if the process died abruptly. */
  char pidfile[PATH_MAX];
  if (resolve_pidfile_from_name(cfg->container_name, pidfile,
                                sizeof(pidfile)) == 0 &&
      access(pidfile, F_OK) == 0)
    unlink(pidfile);

  /* Best-effort cleanup of an empty/unmounted image mountpoint directory. */
  char mnt[PATH_MAX];
  if (snprintf(mnt, sizeof(mnt), "%s/%s", DS_IMG_MOUNT_ROOT_UNIVERSAL,
               safe_name) > 0 &&
      access(mnt, F_OK) == 0 && !is_mountpoint(mnt))
    rmdir(mnt);

  if (remove_recursive(container_dir) != 0) {
    ds_error("Failed to delete container '%s': %s", cfg->container_name,
             strerror(errno));
    goto out;
  }

  ds_log("Deleted container '%s'", cfg->container_name);
  ret = 0;

out:
  ds_container_unlock(lock_fd);
  return ret;
}
