##########################################################################################
#
# Framework Patcher V2 Config Script
#
##########################################################################################

##########################################################################################
# Framework Files Replacement
##########################################################################################

REPLACE="
"
##########################################################################################
# Permissions
##########################################################################################

set_permissions() {
  # Set proper permissions for framework files
  set_perm_recursive $MODPATH/system/framework 0 0 0755 0644 u:object_r:system_file:s0
  set_perm_recursive $MODPATH/system/system_ext/framework 0 0 0755 0644 u:object_r:system_file:s0
}

##########################################################################################
# MMT Extended Logic - Don't modify anything after this
##########################################################################################

SKIPUNZIP=1
unzip -qjo "$ZIPFILE" 'common/functions.sh' -d $TMPDIR >&2
. $TMPDIR/functions.sh
