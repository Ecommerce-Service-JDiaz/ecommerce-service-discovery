# Guía de Despliegue

Esta guía describe el proceso de despliegue automático del servicio de descubrimiento (Service Discovery) en Azure Kubernetes Service (AKS).

## Estructura de Branches y Ambientes

- **`main`** → Despliegue automático a **Production**
- **`staging`** → Despliegue automático a **Staging**
- **`develop`** → Despliegue automático a **Development**

## Pipelines de CI/CD

### Pipeline de Producción (`deploy-prod.yml`)

Se ejecuta automáticamente cuando se hace push a la rama `main` o manualmente mediante `workflow_dispatch`.

**Proceso:**
1. Build del proyecto con Maven
2. Construcción de la imagen Docker
3. Push de la imagen al Azure Container Registry
4. Despliegue en el namespace `production` de AKS
5. Verificación del despliegue

**Características:**
- 3 réplicas para alta disponibilidad
- Recursos: 512Mi-1024Mi RAM, 500m-1000m CPU
- Health checks configurados

### Pipeline de Staging (`deploy-stage.yml`)

Se ejecuta automáticamente cuando se hace push a la rama `staging` o manualmente mediante `workflow_dispatch`.

**Proceso:**
Similar al de producción pero con menos recursos.

**Características:**
- 2 réplicas
- Recursos: 256Mi-512Mi RAM, 250m-500m CPU

### Pipeline de Development (`deploy-dev.yml`)

Se ejecuta automáticamente cuando se hace push a la rama `develop` o manualmente mediante `workflow_dispatch`.

**Proceso:**
Similar a los anteriores pero optimizado para desarrollo.

**Características:**
- 1 réplica
- Recursos: 256Mi-512Mi RAM, 250m-500m CPU
- Health checks con delays más largos

### Pipeline de Pull Requests (`pr-pipeline.yml`)

Se ejecuta automáticamente en cada Pull Request hacia `main`, `staging` o `develop`.

**Proceso:**
1. Ejecución de tests
2. Build del proyecto
3. Construcción de imagen Docker (sin push)
4. Validación de manifiestos de Kubernetes
5. Validación del POM de Maven

**No realiza despliegue**, solo validaciones y tests.

## Manifiestos de Kubernetes

Los manifiestos se encuentran en el directorio `k8s/`:

- `deployment-prod.yaml`: Deployment para producción
- `deployment-stage.yaml`: Deployment para staging
- `deployment-dev.yaml`: Deployment para desarrollo
- `service.yaml`: Service común para todos los ambientes
- `ingress.yaml`: Ingress opcional (ajustar según necesidades)

## Configuración de ConfigMaps

Los ConfigMaps se crean automáticamente durante el despliegue con los archivos de configuración de Spring Boot:
- `application.yml`
- `application-{env}.yml` (según el ambiente)

## Health Checks

El servicio expone endpoints de health checks en `/actuator/health`:
- **Liveness Probe**: `/actuator/health/liveness`
- **Readiness Probe**: `/actuator/health/readiness`

## Monitoreo

Para verificar el estado del despliegue:

```bash
# Ver pods
kubectl get pods -n <namespace>

# Ver servicios
kubectl get svc -n <namespace>

# Ver logs
kubectl logs -f deployment/service-discovery -n <namespace>

# Ver estado del deployment
kubectl rollout status deployment/service-discovery -n <namespace>
```

## Rollback

En caso de problemas, se puede hacer rollback:

```bash
kubectl rollout undo deployment/service-discovery -n <namespace>
```

O a una revisión específica:

```bash
kubectl rollout undo deployment/service-discovery --to-revision=<revision-number> -n <namespace>
```

## Troubleshooting

### Problemas Comunes

1. **Error de autenticación con ACR**
   - Verificar que los secretos `AZURE_REGISTRY_USERNAME` y `AZURE_REGISTRY_PASSWORD` estén correctos
   - Verificar permisos en el ACR

2. **Error de conexión con AKS**
   - Verificar que `AZURE_RESOURCE_GROUP` y `AZURE_AKS_CLUSTER_NAME` sean correctos
   - Verificar permisos del Service Principal

3. **Pods no inician**
   - Revisar logs: `kubectl logs <pod-name> -n <namespace>`
   - Verificar ConfigMap: `kubectl describe configmap service-discovery-config -n <namespace>`
   - Verificar recursos disponibles en el cluster

4. **Health checks fallan**
   - Verificar que el puerto 8761 esté correctamente expuesto
   - Verificar que los endpoints de actuator estén habilitados

## Próximos Pasos

- Configurar Ingress según necesidades (el archivo `ingress.yaml` está listo para personalizar)
- Configurar autoscaling horizontal (HPA) si es necesario
- Configurar Network Policies para seguridad
- Configurar monitoring y alerting (Prometheus, Grafana, etc.)


