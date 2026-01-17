// main.bicep - Taskbook Todo Application Infrastructure
targetScope = 'subscription'

@description('Environment name')
param environmentName string = 'dev'

@description('Location for all resources')
param location string = 'westeurope'

@description('PostgreSQL administrator login')
param postgresAdminLogin string = 'pgadmin'

@secure()
@description('PostgreSQL administrator password')
param postgresAdminPassword string

@secure()
@description('JWT secret for backend authentication')
param jwtSecret string

@description('Unique suffix for globally unique resources')
param uniqueSuffix string = uniqueString(subscription().subscriptionId)

// Resource naming
var resourceGroupName = 'rg-taskbook-${environmentName}'
var containerRegistryName = 'crtaskbook${uniqueSuffix}'
var containerAppsEnvName = 'cae-taskbook-${environmentName}'
var postgresServerName = 'psql-taskbook-${environmentName}-${uniqueSuffix}'
var managedIdentityName = 'id-taskbook-${environmentName}'

// Resource Group
resource rg 'Microsoft.Resources/resourceGroups@2023-07-01' = {
  name: resourceGroupName
  location: location
}

// Managed Identity
module managedIdentity 'modules/managedIdentity.bicep' = {
  scope: rg
  name: 'managedIdentity'
  params: {
    name: managedIdentityName
    location: location
  }
}

// Container Registry
module acr 'modules/containerRegistry.bicep' = {
  scope: rg
  name: 'containerRegistry'
  params: {
    name: containerRegistryName
    location: location
  }
}

// PostgreSQL Flexible Server
module postgres 'modules/postgresql.bicep' = {
  scope: rg
  name: 'postgresql'
  params: {
    serverName: postgresServerName
    location: location
    administratorLogin: postgresAdminLogin
    administratorPassword: postgresAdminPassword
    databaseName: 'tododb'
  }
}

// Container Apps Environment
module containerAppsEnv 'modules/containerAppsEnv.bicep' = {
  scope: rg
  name: 'containerAppsEnv'
  params: {
    name: containerAppsEnvName
    location: location
  }
}

// Backend Container App (internal only - not exposed to internet)
module backendApp 'modules/containerApp.bicep' = {
  scope: rg
  name: 'backendApp'
  params: {
    name: 'ca-taskbook-backend'
    location: location
    containerAppsEnvironmentId: containerAppsEnv.outputs.id
    containerRegistryLoginServer: acr.outputs.loginServer
    containerRegistryUsername: acr.outputs.adminUsername
    containerRegistryPassword: acr.outputs.adminPassword
    imageName: 'mcr.microsoft.com/azuredocs/containerapps-helloworld:latest' // Placeholder - will be updated by pipeline
    targetPort: 8080
    isExternal: false // Internal only - not exposed to internet
    envVars: [
      {
        name: 'SPRING_DATASOURCE_URL'
        value: 'jdbc:postgresql://${postgres.outputs.fqdn}:5432/tododb?sslmode=require'
      }
      {
        name: 'SPRING_DATASOURCE_USERNAME'
        value: postgresAdminLogin
      }
      {
        name: 'SPRING_DATASOURCE_PASSWORD'
        secretRef: 'postgres-password'
      }
      {
        name: 'APP_JWT_SECRET'
        secretRef: 'jwt-secret'
      }
      {
        name: 'APP_CORS_ALLOWED_ORIGINS'
        value: '*' // Allow all origins since frontend proxies through nginx
      }
    ]
    secrets: [
      {
        name: 'postgres-password'
        value: postgresAdminPassword
      }
      {
        name: 'jwt-secret'
        value: jwtSecret
      }
    ]
  }
}

// Frontend Container App (external - exposed to internet)
module frontendApp 'modules/containerApp.bicep' = {
  scope: rg
  name: 'frontendApp'
  params: {
    name: 'ca-taskbook-frontend'
    location: location
    containerAppsEnvironmentId: containerAppsEnv.outputs.id
    containerRegistryLoginServer: acr.outputs.loginServer
    containerRegistryUsername: acr.outputs.adminUsername
    containerRegistryPassword: acr.outputs.adminPassword
    imageName: 'mcr.microsoft.com/azuredocs/containerapps-helloworld:latest' // Placeholder - will be updated by pipeline
    targetPort: 80
    isExternal: true // External - exposed to internet
    envVars: []
    secrets: []
  }
  dependsOn: [backendApp]
}

// Outputs
output resourceGroupName string = rg.name
output containerRegistryLoginServer string = acr.outputs.loginServer
output containerRegistryName string = acr.outputs.name
output backendAppName string = backendApp.outputs.name
output backendInternalFqdn string = backendApp.outputs.fqdn
output frontendAppName string = frontendApp.outputs.name
output frontendUrl string = 'https://${frontendApp.outputs.fqdn}'
output postgresServerFqdn string = postgres.outputs.fqdn
output containerAppsEnvDefaultDomain string = containerAppsEnv.outputs.defaultDomain
