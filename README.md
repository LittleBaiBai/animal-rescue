# Animal Rescue ♥️😺 ♥️🐶 ♥️🐰 ♥️🐦 ♥️🐹

The sample repo for "Bullet-proof Microservices with Spring & Kubernetes" by [Bella Bai](https://github.com/LittleBaiBai) and [Oliver Hughes](https://github.com/ojhughes).

You can also checkout tag `startdemo` the following steps as we were going through in our talk.

Or you can check out the corresponding tag to get the code for different stages:

- `startdemo`: starting point
- `basicauth`: TLS + Ingress + Basic Auth
- `oauth2`: TLS + Ingress + OAuth2 
- `mTLS`: mTLS

Please open an issue/PR if any of the tags or steps are inaccurate.

Have fun rescuing!

## Requirements
* A Kubernetes cluster. Ideally running in a cloud environment (such as Google Kubernetes Engine or VMware TKGi) as the examples require internet facing DNS records.
* kubectl CLI
* helm CLI
    * Add the following repos following the steps:
        ```bash
        helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx  # for nginx ingress
        helm repo add stable https://kubernetes-charts.storage.googleapis.com   # for oauth2-proxy
        helm repo add jetstack https://charts.jetstack.io                       # for cert-manager
        helm repo add bitnami https://charts.bitnami.com/bitnami                # for external-dns
        helm repo add smallstep https://smallstep.github.io/helm-charts/        # for autocert
        ```
* skaffold CLI
* A top level domain name to use (our examples use the domain spring.animalrescue.online, you will need to change this to your own)
* You need to create an NS record for a subdomain of your top level domain. Our examples use an NS record pointing to Google Cloud DNS servers. You can use [any DNS provider in this list](https://github.com/kubernetes-sigs/external-dns#the-latest-release-v07)
* Do a global find and replace in the `k8s` folder: `s/spring.animalrescue.online/yourdomain.com/g`
* Before the deployment, build the frontend artifact:

    ```bash
    cd frontend
    npm install
    npm run build
    ```
* GCP service account secret for using ExternalDNS. See [this section](#externaldns) You can manually create DNS entry and remove ExternalDNS from [skaffold](skaffold.yaml) and [kustomization](k8s/kustomization.yaml)
* OAuth2 application with GitHub or your favorite provider. See [this section](#securing-http-with-oauth2-proxy-with-github)
* OAuth2 application with an OIDC provider. See [this section](#setting-up-internal-oauth-with-an-oidc-provider)
* The guide assume you have `skaffold dev` running to apply changes. You can also run `skaffold run` with each step.

## Basic Auth on API

### Manual basic auth

1. Create secret

    ```bash
    kubectl create secret generic animal-rescue-basic --from-literal=username=alice  --from-literal=password=test
    ```

1. Use the secret in the container, use `basic` profile

    ```bash
    env:
      - name: SPRING_PROFILES_ACTIVE
        value: basic
      - name: ANIMAL_RESCUE_SECURITY_BASIC_PASSWORD
        valueFrom:
          secretKeyRef:
            name: animal-rescue-basic
            key: password
      - name: ANIMAL_RESCUE_SECURITY_BASIC_USERNAME
        valueFrom:
          secretKeyRef:
            name: animal-rescue-basic
            key: username
    ```

1. Deploy and verify basic auth working with the app, and partner app shows 401
1. Add basic auth configuration to external API deployment

    ```yaml
              - name: ANIMAL_RESCUE_PASSWORD
                valueFrom:
                  secretKeyRef:
                    name: animal-rescue-basic
                    key: password
              - name: ANIMAL_RESCUE_USERNAME
                valueFrom:
                  secretKeyRef:
                    name: animal-rescue-basic
                    key: username
    ```

1. Edit server.js to use basic auth from secret

    ```js
    const response = await axios.get(`${animalRescueBaseUrl}/api/animals`, {
        auth: {
            username: animalRescueUsername,
            password: animalRescuePassword
        }
    });
    ```

1. Partner API use the same secret to access `/api/animals` endpoint
1. Change the secret and rolling restart

### Ingress + Basic Auth

1. Start fresh from the beginning.
1. Uncomment the ingress release in `deploy/helm/releases` section in [skaffold.yaml](./skaffold.yaml)
1. Create secret
   
    Generate `auth` file using the following command:
    
    ```bash
    mkdir k8s/ingress/secret
    cd k8s/ingress/secret
    htpasswd -c auth alice # Password is MD5 encrypted by default
    ```
    
    Create secret with `secretGenerator` in [ingress kustomization](./k8s/ingress/kustomization.yaml):
    
    ```yaml
    secretGenerator:
    - name: ingress-basic-auth
      type: Opaque
      files:
      - secret/auth
    
    generatorOptions:
      disableNameSuffixHash: true
    ```

1. Add basic auth annotation to [animal-rescue-ingress yaml](./k8s/ingress/animal-rescue-ingress.yaml)

    ```yaml
      annotations:
        # type of authentication
        nginx.ingress.kubernetes.io/auth-type: basic
        # name of the secret that contains the user/password definitions
        nginx.ingress.kubernetes.io/auth-secret: ingress-basic-auth
    ```

1. Uncomment `- k8s/` from `deploy.kustomize.paths` section in [skaffold.yaml](./skaffold.yaml) to include the ingress yaml
1. You need DNS records `[spring.yourdomain | partner.spring.yourdomain | auth-external.yourdomain]` and assign them to the ingress IP address. If you don't want to do it manually, you can use [ExternalDNS](#ExternalDNS) to generate DNS entry.

_Note for uninstall: you may need to fun the following command after `helm uninstall`_

```bash
kubectl delete -A ValidatingWebhookConfiguration ingress-s1p-ingress-nginx-admission
```

_Note for running on GKE: I had to run the following command to enable webhook:_

```bash
kubectl create clusterrolebinding cluster-admin-binding \
  --clusterrole cluster-admin \
  --user $(gcloud config get-value account)
```

### ExternalDNS

[Helm install](https://github.com/bitnami/charts/tree/master/bitnami/external-dns)

[Example helm values](https://github.com/paulczar/platform-operations-on-kubernetes/blob/master/charts/externaldns/helmfile/base.yaml.gotmpl)

1. Set up a hosted DNS with the provider of your choice. We use Google DNS. More info in [this section](#google-dns-setup)
1. Pull down the service account json with `gcloud` cli :
    ```bash
    gcloud iam service-accounts keys create ./k8s/external-dns/secret/gcp-dns-account-credentials.json --iam-account=$CLOUD_DNS_SA
    ```
1. Update [helm values](./k8s/external-dns/external-dns-helm-values.yaml) for your DNS provider. ExternalDNS has more information on integration with different providers in their[doc](https://github.com/kubernetes-sigs/external-dns)
1. Uncomment `resources[external-dns]` in [k8s/kustomization.yaml](./k8s/kustomization.yaml)
1. Uncomment the ingress release in `deploy/helm/releases` section in [skaffold.yaml](./skaffold.yaml)

Watch on DNS zone changes: 

```bash
watch gcloud dns record-sets list --zone animal-rescue-zone 
```

Tail logs with [stern](https://github.com/wercker/stern):

```bash
stern external -n external-dns
```

The record will take a few minute to propogate. Keep pinging...

### TLS

1. Uncomment the cert-manager release in `deploy/helm/releases` section in [skaffold.yaml](./skaffold.yaml)
1. Include `letsencrypt-staging.yaml` in [ingress kustomization](./k8s/ingress/kustomization.yaml)
1. Update [ingress](./k8s/ingress/animal-rescue-ingress.yaml), uncomment code following the `TODO`s
    ```yaml
    annotations:
     # In addition to existing auth annotations
     cert-manager.io/cluster-issuer: "letsencrypt"
     kubernetes.io/tls-acme: "true" # This annotation tells ingress to exclude that acme challenge path from authentication.
    
    # Add TLS enforcement
    spec:
     tls:
       - hosts:
           - spring.animalrescue.online
         secretName: animal-rescue-certs
       - hosts:
           - partner.spring.animalrescue.online
         secretName: partner-certs
    ```

Check ingress gets certificate related events:

```bash
kubectl describe ingress animal-rescue-ingress
```

Check certificate status:

```bash
kubectl get certificates
```

Verify TLS:

```bash
curl http://spring.animalrescue.online/api/animals # Should get `308 Permanent Redirect` back
curl https://spring.animalrescue.online/api/animals -k # Should get `401` back
curl https://spring.animalrescue.online/api/animals -k --user alice:test # Should get `200`response back
```

After verified that everything works fine, switch to use prod server.

- Add [letsencrypt-prod.yaml](./k8s/ingress/letsencrypt-prod.yaml) to [kustomization](./k8s/ingress/kustomization.yaml)
- Use the prod issuer in ingress
- Update the secret names in the TLS section so cert manager can create new ones.

Visit the site again to see a trusted cert!

### OAuth2

#### Securing HTTP with oauth2-proxy with GitHub

1. Create an OAuth application in the GitHub website (or another OAuth provider such as google if you prefer but you will need to change the settings in `k8s/oauth2-proxy/external-oauth2-proxy-helm-values.yaml`) 
   See the [GitHub guide for setting up an OAuth application](https://developer.github.com/apps/building-oauth-apps/creating-an-oauth-app/)
1. When creating the OAuth App in GitHub, make sure the Authorization Callback URL points at your own domain eg: https://auth-external.spring.animalrescue.online/oauth2/callback
1. Create the directory `k8s/oauth2-proxy/secret` (it is git ignored)
1. Create the file `k8s/oauth2-proxy/secret/oauth2-external-proxy-creds`, using the structure below;

    ```properties
    cookie-secret=<<random value>>
    client-id=<<Your client ID from the GitHub OAuth application page>>
    client-secret=<<Your client secret from the GitHub OAuth application page>>
    ```
1. In the Helm values file `k8s/oauth2-proxy/external-oauth2-proxy-helm-values.yaml` change the email under `restricted_access:` to the email of your GitHub account

#### Setting up internal OAuth with an OIDC provider

The idea of this section is that you may want to protect internal resources using a different auth backend.

In our example we are using [TKGi Kubernetes](https://docs.pivotal.io/tkgi/1-8/index.html) and its' internal UAA OAuth2 provider.
You will need to modify the configuration in `k8s/oauth2-proxy/internal-oauth2-proxy-helm-values.yaml` and
change `oidc-issuer-url` to point at your own service. The create an OAuth Client and add the client id and secret to the file 
`k8s/oauth2-proxy/secret/oauth2-internal-proxy-creds`.

Then accessing 

#### Install both oauth2-proxy and use them with ingress

1. Include `oauth2-proxy` in [k8s/kustomization](k8s/kustomization.yaml) 
1. Include oauth2 ingresses in [ingress/kustomization](k8s/ingress/kustomization.yaml)
1. Uncomment the 2 `oauth2-proxy` releases in `deploy/helm/releases` section in [skaffold.yaml](./skaffold.yaml)
1. Open a browser to `yourdomain.com` and you should be presented with a GitHub authorization page and redirected to the animal-rescue site
1. Accessing any actuator endpoint `yourdomain.com/actuator/*` and you should be redirected to your choice of internal auth provider for authentication then redirected to the actuator endpoint.

### mTLS with Autocert

[Doc](https://github.com/smallstep/autocert)

1. Uncomment the `autocert` release in `deploy/helm/releases` section in [skaffold.yaml](./skaffold.yaml)
1. Label the namespace to enable autocert:
    
    ```bash
    kubectl label namespace animal-rescue autocert.step.sm=enabled
    ```
    
    Or Add the following metadata to [namespace.yaml](external-api/k8s/namespace.yaml)
    
    ```yaml
    metadata:
      # Additional to existing metadata
      labels:
        autocert.step.sm: enabled
    ``` 
   
1. Annotate the node app
    
   ```yaml   
    spec:
      template:
        metadata:
          # Additional to existing metadata
          annotations:
            autocert.step.sm/name: partner-adoption-center.animal-rescue.svc.cluster.local
    ```

1. Check certs on container:

    ```bash
    set PARTNER_POD (kubectl get pods -l app=partner-adoption-center -o jsonpath='{$.items[0].metadata.name}')
    kubectl exec -it $PARTNER_POD -c partner-adoption-center -- ls /var/run/autocert.step.sm
    # Should see root.crt  site.crt  site.key
    kubectl exec -it $PARTNER_POD -c partner-adoption-center -- cat /var/run/autocert.step.sm/site.crt | step certificate inspect --short -
    # We can see the subject is what we set in the annotation, and the cert is valid for a day. Then the sidecar will take care of the renewal
    ```
 
1. Update the node app to be mTLS enabled. You can compare the file with the branch `mtls` to get the code that works, or follow this [code example](https://github.com/smallstep/autocert/blob/master/examples/hello-mtls/node/server.js) to come up with your own.
1. Update [service](external-api/k8s/service.yaml) port from `80` to `443`
1. Deploy a CURL mTLS client:

    ```bash
    kubectl apply -f ./external-api/k8s/curl-mtls-client.yaml
    ```

    Or add it through kustomization.
    
1. Verify it work by checking the logs:

    ```bash
    stern curl-mtls-client
    ```
 
    Or do the curl manually: 
    
    ```bash
    set MTLS_CLIENT (kubectl get pods -l app=curl-mtls-client -o jsonpath='{$.items[0].metadata.name}')
    
    k exec -it $MTLS_CLIENT -- curl -sS \
           --cacert /var/run/autocert.step.sm/root.crt \
           --cert /var/run/autocert.step.sm/site.crt \
           --key /var/run/autocert.step.sm/site.key \
           https://partner-adoption-center.animal-rescue.svc.cluster.local
    # Should return 200 with response
    
    kubectl exec -it $MTLS_CLIENT -- curl https://partner-adoption-center.animal-rescue.svc.cluster.local
    # Should fail on server cert validation
   
    kubectl exec -it $MTLS_CLIENT -- curl https://partner-adoption-center.animal-rescue.svc.cluster.local -k
    # Should fail on client cert validation
    ```

### Google DNS setup 
The kNative project has a good guide for setting up external DNS with Google Cloud that you can follow in order to get a K8s cluster up and running 
[https://knative.dev/v0.15-docs/serving/using-external-dns-on-gcp/#set-up-kubernetes-engine-cluster-with-clouddns-readwrite-permissions]()
