## About Velocity support

**AuthMeReReloaded offers integrated support for Velocity since b46, in order to use, please follow the instructions below.**


### Step 1: Enable Velocity hook in config.yml

```yaml
Hooks:
    # Do we need to hook with Velocity?
    velocity: true # Set this to true
```
**Then restart your server if you have done so.**

### Step 2: Configure backend server and proxy

**Before we can continue, you need to configure your Velocity proxy forwarding method following [PaperMC's instructions](https://docs.papermc.io/velocity/player-information-forwarding#configuring-modern-forwarding).**

### Step 3: Install AuthMeVelocity on the Proxy

AuthMeVelocity is a plugin that allows Velocity to forward player information to AuthMeReloaded.

**Only install it on the proxy, not on the backend server.**

**You can download it from [Modrinth](https://modrinth.com/plugin/authmevelocity).**

**Then configure follow its instructions.**
