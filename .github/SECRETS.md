# Configuración de Secretos de GitHub Actions

Este documento describe los secretos necesarios para configurar los pipelines de CI/CD en GitHub Actions.

## Secretos Requeridos

Los siguientes secretos deben ser configurados en GitHub Actions (Settings > Secrets and variables > Actions):

### Docker Hub

- **`DOCKERHUB_USERNAME`**: Tu nombre de usuario de Docker Hub
  - Ejemplo: `tu-usuario`
  - Se obtiene desde tu cuenta de Docker Hub

- **`DOCKERHUB_TOKEN`**: Token de acceso de Docker Hub (recomendado) o contraseña
  - Se obtiene desde Docker Hub > Account Settings > Security > New Access Token
  - **Recomendación**: Usa un Access Token en lugar de tu contraseña por seguridad
  - Si usas contraseña, asegúrate de que tu cuenta tenga 2FA deshabilitado (no recomendado)

### Azure Kubernetes Service (AKS)

- **`AZURE_CREDENTIALS`**: Credenciales del Service Principal de Azure en formato JSON
  - Este es el secreto más importante para autenticarse con Azure
  - Formato JSON con las siguientes propiedades:
    ```json
    {
      "clientId": "xxxx-xxxx-xxxx-xxxx",
      "clientSecret": "xxxx-xxxx-xxxx-xxxx",
      "subscriptionId": "xxxx-xxxx-xxxx-xxxx",
      "tenantId": "xxxx-xxxx-xxxx-xxxx"
    }
    ```
  - **Cómo obtenerlo**: Ver sección "Cómo crear un Service Principal" más abajo

- **`AZURE_RESOURCE_GROUP_PROD`**: Nombre del grupo de recursos de **Producción** donde está el cluster AKS
  - Ejemplo: `my-resource-group-prod`
  - Usado por el workflow `deploy-prod.yml`

- **`AZURE_AKS_CLUSTER_NAME_PROD`**: Nombre del cluster de AKS de **Producción**
  - Ejemplo: `my-aks-cluster-prod`
  - Usado por el workflow `deploy-prod.yml`

- **`AZURE_RESOURCE_GROUP_STAGE`**: Nombre del grupo de recursos de **Staging** donde está el cluster AKS
  - Ejemplo: `my-resource-group-stage`
  - Usado por el workflow `deploy-stage.yml`

- **`AZURE_AKS_CLUSTER_NAME_STAGE`**: Nombre del cluster de AKS de **Staging**
  - Ejemplo: `my-aks-cluster-stage`
  - Usado por el workflow `deploy-stage.yml`

- **`AZURE_RESOURCE_GROUP_DEV`**: Nombre del grupo de recursos de **Desarrollo** donde está el cluster AKS
  - Ejemplo: `my-resource-group-dev`
  - Usado por el workflow `deploy-dev.yml`

- **`AZURE_AKS_CLUSTER_NAME_DEV`**: Nombre del cluster de AKS de **Desarrollo**
  - Ejemplo: `my-aks-cluster-dev`
  - Usado por el workflow `deploy-dev.yml`

**Nota**: Cada ambiente tiene su propio Resource Group y cluster AKS, por lo que necesitas configurar secretos separados para cada uno.

### Zipkin (Distributed Tracing)

- **`ZIPKIN_BASE_URL`**: URL base del servidor Zipkin (compartida para todos los ambientes)
  - Ejemplo si Zipkin está en el mismo cluster: `http://zipkin-service.zipkin.svc.cluster.local:9411/`
  - Ejemplo si Zipkin está expuesto externamente: `https://zipkin.example.com/`
  - **Importante**: La URL debe terminar con `/` (barra diagonal)
  - Esta variable se inyecta como `SPRING_ZIPKIN_BASE_URL` en todos los deployments
  - Cada servicio llamará a Zipkin desde su propio namespace usando esta URL base

**Nota sobre Zipkin:**
- Si Zipkin está desplegado en el mismo cluster de Kubernetes, usa el formato de servicio interno: `http://zipkin-service.{namespace-zipkin}.svc.cluster.local:9411/`
- Si Zipkin está desplegado externamente, usa la URL completa con protocolo: `https://zipkin.example.com/`
- Solo necesitas una variable porque todos los servicios usan la misma URL base para conectarse a Zipkin

## Configuración de Environments

En GitHub Actions, se deben configurar los siguientes environments con sus respectivas protecciones:

### Production Environment
- **Nombre**: `production`
- **Branch protection**: Solo desde `main`
- **Required reviewers**: Configurar según políticas de la organización



### Staging Environment
- **Nombre**: `staging`
- **Branch protection**: Solo desde `staging`

### Development Environment
- **Nombre**: `development`
- **Branch protection**: Solo desde `develop`

## Cómo Configurar los Secretos

1. Ve a tu repositorio en GitHub
2. Navega a **Settings** > **Secrets and variables** > **Actions**
3. Haz clic en **New repository secret**
4. Agrega cada uno de los secretos listados arriba
5. Para los environments, ve a **Environments** y crea los tres environments mencionados

## Cómo Crear un Service Principal

Para obtener las credenciales de Azure (`AZURE_CREDENTIALS`), necesitas crear un Service Principal:

### Opción 1: Usando Azure CLI

```bash
# Login a Azure
az login

# Crear Service Principal con permisos en TODOS los Resource Groups
# Opción 1: Un solo SP con permisos en todos los resource groups
az ad sp create-for-rbac --name "github-actions-sp" \
  --role contributor \
  --scopes /subscriptions/{subscription-id}/resourceGroups/{resource-group-prod} \
           /subscriptions/{subscription-id}/resourceGroups/{resource-group-stage} \
           /subscriptions/{subscription-id}/resourceGroups/{resource-group-dev} \
  --sdk-auth

# Asignar el rol de AKS Cluster User para cada cluster
az role assignment create \
  --assignee <clientId-del-sp> \
  --role "Azure Kubernetes Service Cluster User Role" \
  --scope /subscriptions/{subscription-id}/resourceGroups/{resource-group-prod}/providers/Microsoft.ContainerService/managedClusters/{cluster-name-prod}

az role assignment create \
  --assignee <clientId-del-sp> \
  --role "Azure Kubernetes Service Cluster User Role" \
  --scope /subscriptions/{subscription-id}/resourceGroups/{resource-group-stage}/providers/Microsoft.ContainerService/managedClusters/{cluster-name-stage}

az role assignment create \
  --assignee <clientId-del-sp> \
  --role "Azure Kubernetes Service Cluster User Role" \
  --scope /subscriptions/{subscription-id}/resourceGroups/{resource-group-dev}/providers/Microsoft.ContainerService/managedClusters/{cluster-name-dev}
```

El comando `az ad sp create-for-rbac --sdk-auth` te dará el JSON que necesitas copiar directamente como el secreto `AZURE_CREDENTIALS`.

### Opción 2: Desde Azure Portal

1. Ve a **Azure Active Directory** > **App registrations** > **New registration**
2. Crea una nueva aplicación
3. Ve a **Certificates & secrets** y crea un nuevo client secret
4. Copia el **Application (client) ID** y el **Directory (tenant) ID**
5. Asigna los roles necesarios (ver Permisos Adicionales)
6. Formatea el JSON con estos valores

## Permisos Adicionales

Asegúrate de que el Service Principal tenga los siguientes permisos en Azure **para cada ambiente**:

### Para cada Resource Group (PROD, STAGE, DEV):
- **Contributor** o **Owner** en el Resource Group del AKS

### Para cada Cluster AKS (PROD, STAGE, DEV):
- **Azure Kubernetes Service Cluster User Role** en el cluster AKS

**Ejemplo de comandos para asignar permisos:**

```bash
# Para Production
az role assignment create \
  --assignee <clientId> \
  --role "Azure Kubernetes Service Cluster User Role" \
  --scope /subscriptions/{subscription-id}/resourceGroups/{resource-group-prod}/providers/Microsoft.ContainerService/managedClusters/{cluster-name-prod}

# Para Staging
az role assignment create \
  --assignee <clientId> \
  --role "Azure Kubernetes Service Cluster User Role" \
  --scope /subscriptions/{subscription-id}/resourceGroups/{resource-group-stage}/providers/Microsoft.ContainerService/managedClusters/{cluster-name-stage}

# Para Development
az role assignment create \
  --assignee <clientId> \
  --role "Azure Kubernetes Service Cluster User Role" \
  --scope /subscriptions/{subscription-id}/resourceGroups/{resource-group-dev}/providers/Microsoft.ContainerService/managedClusters/{cluster-name-dev}
```

**Nota sobre Docker Hub:**
- Si tus imágenes son **públicas**, no necesitas configuración adicional en Kubernetes
- Si tus imágenes son **privadas**, el pipeline creará automáticamente un Secret en Kubernetes para autenticarse con Docker Hub

## Verificación

Para verificar que los secretos están configurados correctamente:

1. Ejecuta manualmente un workflow usando `workflow_dispatch`
2. Revisa los logs del workflow para asegurarte de que no hay errores de autenticación
3. Verifica que las imágenes se suben correctamente a Docker Hub
4. Confirma que el despliegue en Kubernetes se realiza sin errores

