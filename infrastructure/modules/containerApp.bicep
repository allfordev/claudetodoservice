@description('Name of the Container App')
param name string

@description('Location')
param location string

@description('Container Apps Environment ID')
param containerAppsEnvironmentId string

@description('Container Registry Login Server')
param containerRegistryLoginServer string

@description('Container Registry Username')
param containerRegistryUsername string

@secure()
@description('Container Registry Password')
param containerRegistryPassword string

@description('Container image name with tag')
param imageName string

@description('Target port for the container')
param targetPort int

@description('Whether ingress is external (internet-facing)')
param isExternal bool

@description('Environment variables')
param envVars array = []

@description('Secrets')
param secrets array = []

// Combine ACR password with other secrets
var allSecrets = concat(secrets, [
  {
    name: 'acr-password'
    value: containerRegistryPassword
  }
])

resource containerApp 'Microsoft.App/containerApps@2023-05-01' = {
  name: name
  location: location
  properties: {
    managedEnvironmentId: containerAppsEnvironmentId
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: isExternal
        targetPort: targetPort
        transport: 'auto'
        allowInsecure: false
      }
      registries: [
        {
          server: containerRegistryLoginServer
          username: containerRegistryUsername
          passwordSecretRef: 'acr-password'
        }
      ]
      secrets: allSecrets
    }
    template: {
      containers: [
        {
          name: name
          image: imageName
          resources: {
            cpu: json('0.5')
            memory: '1Gi'
          }
          env: envVars
        }
      ]
      scale: {
        minReplicas: 1
        maxReplicas: 3
      }
    }
  }
}

output id string = containerApp.id
output name string = containerApp.name
output fqdn string = containerApp.properties.configuration.ingress.fqdn
