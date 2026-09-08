# React Native 0.86 Trusted Publishing Backport

## Objective

Create `fix/0.86-publishing` from `origin/0.86-stable` and backport the complete
npm trusted-publishing implementation from `0.87-stable` without bringing in
unrelated 0.87 release or Hermes refactors.

The repository must be opened with this workspace root before implementation:

```text
/Users/cipolleschi/Official/react-native
```

The previous session was rooted at `packages/rn-tester`, so Git metadata and
`.github` were outside the writable workspace.

## Investigation Summary

The failed `v0.86.1` job stopped before npm publication:

```text
Execution failed for task ':initializeSonatypeStagingRepository'.
Failed to find staging profile for package group: com.facebook.react
```

This is a Sonatype credential or namespace-permission problem, not an npm OIDC
failure. The successful `v0.86.0` run used the same Maven code and endpoint.
Repository secrets survived the GitHub organization transfer and were non-empty
in the failed run, but the Sonatype identity no longer resolved the
`com.facebook` staging profile.

Separately, `0.86-stable` still uses `GHA_NPM_TOKEN`. It needs the 0.87 trusted
publishing implementation so subsequent 0.86 releases use OIDC.

References:

- Failed run: <https://github.com/react/react-native/actions/runs/30060604566/job/89384456945>
- Successful 0.86.0 run: <https://github.com/react/react-native/actions/runs/27205634477>

## Source Of Truth

Compare `0.86-stable` with `0.87-stable`. Port the final behavior introduced by:

- `8bcfb3ba1c1` - initial npm OIDC trusted-publishing migration
- `567b9f0aa15` - consolidate all npm publishing into one top-level workflow
- `65bdf260833` - use Node 24/npm 11.5+ for trusted publishing
- `c6110b1a3fc` - remove temporary OIDC debugging

Do not port the broad YAML formatting commit `738839c0dd4`.

## Implementation

1. Create the branch:

   ```bash
   git switch -c fix/0.86-publishing origin/0.86-stable
   ```

2. Add `.github/workflows/publish-npm.yml` based on `0.87-stable`. It must be
   the single top-level workflow for all npm publication:

   - Release tag pushes select `release` mode.
   - Scheduled and manual runs select `nightly` mode.
   - Pushes to `main` and `*-stable` select `bumped-packages` mode.
   - `publish_react_native` and `publish_bumped_packages` use the
     `npm-publish` environment.
   - Both npm publishing jobs grant `contents: read` and `id-token: write`.
   - Node publishing uses Node 24 and `https://registry.npmjs.org`.
   - Preserve the existing Maven credentials and publication sequence.
   - Preserve all existing release post-processing jobs.

3. Adapt the unified workflow to 0.86 interfaces rather than porting 0.87-only
   Hermes changes:

   - Keep the `set_hermes_versions` job and extract both `HERMES_VERSION` and
     `HERMES_V1_VERSION`.
   - Pass both `hermesVersion` and `hermesV1Version` to
     `create-draft-release.yml`.
   - Call `prebuild-ios-core.yml` with the existing 0.86 input
     `use-hermes-nightly`, not the 0.87 `use-hermes-prebuilt` input.
   - Retain `version-type: nightly` for nightly Apple prebuilds.
   - Do not add the unrelated `skip-apple-prebuilts` behavior.

4. Update `.github/actions/setup-node/action.yml`:

   - Add an optional `registry-url` input with an empty default.
   - Forward it to `actions/setup-node@v6`.

5. Update `.github/actions/build-npm-package/action.yml`:

   - Remove the `gha-npm-token` input.
   - Remove the step that writes token authentication to `.npmrc`.
   - Configure the shared Node action with `node-version: '24'` and
     `registry-url: 'https://registry.npmjs.org'`.
   - Document that the caller must grant `id-token: write`.
   - Leave the remaining 0.86 artifact and publication behavior unchanged.

6. Update `.github/workflows/create-release.yml`:

   - Remove the legacy `GHA_NPM_TOKEN` validation step and environment value.

7. Delete the workflows replaced by the unified entry point:

   - `.github/workflows/nightly.yml`
   - `.github/workflows/publish-release.yml`
   - `.github/workflows/publish-bumped-packages.yml`

## Explicit Exclusions

- No Sonatype preflight workflow. That is not part of the 0.87 trusted
  publishing implementation.
- No JavaScript release-script changes.
- No Hermes utility renames or consolidated-version refactors.
- No Apple-prebuild skipping behavior.
- No unrelated YAML formatting.
- No changes to Maven coordinates, npm tags, or package contents.

## Verification

1. Run formatting checks on the changed YAML files.
2. Run `git diff --check`.
3. Run the existing release publishing test:

   ```bash
   yarn test scripts/releases-ci/__tests__/publish-npm-test.js --runInBand
   ```

4. Search the publishing surface and confirm no workflow still references
   `GHA_NPM_TOKEN`.
5. Compare triggers, job dependencies, environments, secrets, permissions, and
   post-release jobs with `0.87-stable`.
6. Confirm the only intentional differences from 0.87 are the 0.86 Hermes and
   Apple-prebuild interfaces listed above.
7. Review the final diff against `origin/0.86-stable` and commit it on
   `fix/0.86-publishing`.

OIDC cannot be tested end to end without a real npm publication. npm must have a
trusted publisher configured for repository `react/react-native`, workflow
`publish-npm.yml`, and environment `npm-publish`.

## Operational Follow-up For v0.86.1

The code backport will not alter the existing `v0.86.1` tag or its failed run.
To recover that run:

1. Generate a Sonatype Central Portal token for an account with access to the
   `com.facebook` namespace.
2. Replace `ORG_GRADLE_PROJECT_SONATYPE_USERNAME` and
   `ORG_GRADLE_PROJECT_SONATYPE_PASSWORD` in the repository secrets.
3. Validate access with `./gradlew retrieveSonatypeStagingProfile --no-daemon`.
4. Confirm the legacy `GHA_NPM_TOKEN` remains valid for the tagged 0.86.1
   workflow.
5. Rerun the failed jobs. The original run failed before creating a Maven
   staging repository and before npm publication.
