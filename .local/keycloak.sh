#!/bin/bash

logInfo() {
    printf "ℹ️ [INFO]  %s\n" "$1"
}

logWarn() {
    printf "⚠️ [WARN]  %s\n" "$1"
}

logError() {
    printf "❌ [ERROR] %s\n" "$1"
}

CONFIDENTIAL_CLIENT='confidential-oidc-client'
PUBLIC_CLIENT='public-oidc-client'
WEB_ORIGINS='["*"]'
REDIRECT_URIS='[
  "http://localhost:18080/home",
  "http://localhost:8092/oauth2-redirect.html"
]'

USERS_GROUPS=(
  "dev1:developers"
  "dev2:developers"
  "view1:viewers"
  "adm1:admins"
)
USERS_PWD=user

get_client_id() {
  local client_name=$1
  /opt/keycloak/bin/kcadm.sh get clients -r master --fields id,clientId \
    | grep -B1 "\"clientId\" : \"${client_name}\"" \
    | grep '"id"' \
    | sed -E 's/.*"id" : "([^"]+)".*/\1/'
}

logInfo "Creating users, roles and clients ..."

# Connect to kecloak
logInfo "Connecting to kecloak ..."
/opt/keycloak/bin/kcadm.sh config credentials --server http://keycloak:$KC_HOSTNAME_PORT \
    --realm master --user $KC_BOOTSTRAP_ADMIN_USERNAME --password $KC_BOOTSTRAP_ADMIN_PASSWORD

# Create Groups and roles
for u in "${USERS_GROUPS[@]}"; do
  group="${u##*:}"
  logInfo "Creating group: $group ..."
  /opt/keycloak/bin/kcadm.sh create groups -r master -s name="$group"

  logInfo "Creating role: $group ..."
  /opt/keycloak/bin/kcadm.sh create roles -r master -s name="$group"
done | sort -u

# Create Users
logInfo "Creating Users and assigning roles ..."
for u in "${USERS_GROUPS[@]}"; do
  user="${u%%:*}"
  group="${u##*:}"
  logInfo "Creating user: $user.$group@example.org ($USERS_PWD) ..."
  /opt/keycloak/bin/kcadm.sh create users -r master -s username="$user" -s firstName="$user" -s lastName="$user" -s enabled=true \
    -s email="$user.$group@example.org" -s emailVerified=true --output
  /opt/keycloak/bin/kcadm.sh set-password -r master --username "$user" --new-password "$USERS_PWD"

  logInfo "Assigning user $user to role $group ..."
  /opt/keycloak/bin/kcadm.sh add-roles -r master --uusername "$user" --rolename "$group"

  logInfo "Finding IDs for user=$user and group=$group ..."
  USER_ID=$(/opt/keycloak/bin/kcadm.sh get users -r master --query "exact=true" --query "username=$user" --fields id --format csv --noquotes)
  GROUP_ID=$(/opt/keycloak/bin/kcadm.sh get groups -r master --query "exact=true" --query "search=$group" --fields id --format csv --noquotes)
  if [[ -n "$USER_ID" && -n "$GROUP_ID" ]]; then
    logInfo "Assigning user $user ($USER_ID) to group $group ($GROUP_ID) ..."
    for i in {1..3}; do
      /opt/keycloak/bin/kcadm.sh update "users/$USER_ID/groups/$GROUP_ID" \
          -r master \
          --set "userId=$USER_ID" \
          --set "groupId=$GROUP_ID" \
          --no-merge && break
      logWarn "⚠️ Attempt $i failed, retrying in 2 seconds..."
      sleep 2
    done
  else
    logError "❌ Could not assign $user to $group (missing IDs)"
  fi
done

# Create OAuth2 clients
logInfo "Creating OAuth2 clients ..."
/opt/keycloak/bin/kcadm.sh create clients -r master -s clientId=$PUBLIC_CLIENT -s name=$PUBLIC_CLIENT -s publicClient=true \
                           -s "redirectUris=${REDIRECT_URIS}" \
                           -s "webOrigins=${WEB_ORIGINS}" \
                           --output
/opt/keycloak/bin/kcadm.sh create clients -r master -s clientId=$CONFIDENTIAL_CLIENT -s name=$CONFIDENTIAL_CLIENT  -s 'secret=secret1' \
                           -s "redirectUris=${REDIRECT_URIS}" \
                           -s "webOrigins=${WEB_ORIGINS}" \
                           --output

logInfo "Updating $CONFIDENTIAL_CLIENT with redirect uris ..."
CONF_CLIENT_ID=$(get_client_id "$CONFIDENTIAL_CLIENT")
/opt/keycloak/bin/kcadm.sh update "clients/$CONF_CLIENT_ID" -r master \
  -s "redirectUris=${REDIRECT_URIS}" \
  -s "webOrigins=${WEB_ORIGINS}" \
  --output

logInfo "Updating $PUBLIC_CLIENT with redirect uris ..."
PUB_CLIENT_ID=$(get_client_id "$PUBLIC_CLIENT")
/opt/keycloak/bin/kcadm.sh update "clients/$PUB_CLIENT_ID" -r master \
  -s "redirectUris=${REDIRECT_URIS}" \
  -s "webOrigins=${WEB_ORIGINS}"  \
  --output

CLIENTS=(
  "$PUBLIC_CLIENT:$PUB_CLIENT_ID"
  "$CONFIDENTIAL_CLIENT:$CONF_CLIENT_ID"
)

# https://medium.com/norsk-helsenett/keycloak-cli-adding-user-to-group-without-sed-3bab247796dd
# Keycloak does not support a groups scope by default.
for client in "${CLIENTS[@]}"; do
  client_name="${client%%:*}"
  client_id="${client##*:}"
  logInfo "Creating group mapper for $client_name client so that groups gets included in tokens"
  /opt/keycloak/bin/kcadm.sh create "clients/$client_id/protocol-mappers/models" \
      -r master \
      --set "name=groups" \
      --set "protocol=openid-connect" \
      --set "protocolMapper=oidc-group-membership-mapper" \
      --set "config.\"full.path\"=false" \
      --set "config.\"introspection.token.claim\"=true" \
      --set "config.\"multivalued\"=true" \
      --set "config.\"id.token.claim\"=true" \
      --set "config.\"access.token.claim\"=true" \
      --set "config.\"userinfo.token.claim\"=true" \
      --set "config.\"claim.name\"=groups" \
      --set "config.\"jsonType.label\"=string" \
      --output
  logInfo "Creating role mapper for $client_name client so that realm roles gets included in tokens"
  /opt/keycloak/bin/kcadm.sh create "clients/$client_id/protocol-mappers/models" \
      -r master \
      --set "name=roles" \
      --set "protocol=openid-connect" \
      --set "protocolMapper=oidc-usermodel-realm-role-mapper" \
      --set "config.\"introspection.token.claim\"=true" \
      --set "config.\"multivalued\"=true" \
      --set "config.\"id.token.claim\"=true" \
      --set "config.\"access.token.claim\"=true" \
      --set "config.\"claim.name\"=roles" \
      --set "config.\"jsonType.label\"=string" \
      --output
  logInfo "Creating role mapper for $client_name client so that client roles gets included in tokens"
  /opt/keycloak/bin/kcadm.sh create "clients/$client_id/protocol-mappers/models" \
      -r master \
      --set "name=roles" \
      --set "protocol=openid-connect" \
      --set "protocolMapper=oidc-usermodel-client-role-mapper" \
      --set "config.\"introspection.token.claim\"=true" \
      --set "config.\"multivalued\"=true" \
      --set "config.\"id.token.claim\"=true" \
      --set "config.\"access.token.claim\"=true" \
      --set "config.\"claim.name\"=roles" \
      --set "config.\"jsonType.label\"=string" \
      --output
done

# Update access token lifetime
logInfo "Updating access token lifetime to 8H ..."
/opt/keycloak/bin/kcadm.sh update realms/master -s accessTokenLifespan=28800
logInfo "Users, roles and clients created successfuly"
exit 0
