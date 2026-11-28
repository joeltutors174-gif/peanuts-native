# Notes for agents

- This repository is Banana Native, moceure's Android compatibility layer for Switch 2 titles, built on [nxnative](https://github.com/seedds/nxnative). Like Wine, it supplies the system rather than modelling hardware: the game's own ARM64 code runs natively on the phone's CPU, rewritten only where the host requires it (`dm/tools/patch_nso.py --target android`), in a process whose host answers Horizon's SVCs and the SDK's services (`dm/host`, `android/host`), with NVN translated to Vulkan (`dm/gfx/vk_android`) and the game's shaders translated ahead of time to SPIR-V (`dm/gfx/tools`). `android/app` is the launcher; `scripts/pack_android.py` prepares a game's pack. nxnative's Mac and iOS host and tooling stay as the upstream toolchain: the shader chain's front half, bring-up and debugging on the Mac. Player pages: [.github/README.md](docs/banana-native-README.github.md) and [README.md](README.md);<!--PEANUTS--> Android internals and building: [android/README.md](android/README.md). The [games table](dm/games/README.md#games) lists every profile and links its note, `dm/games/<id>/README.md`.
- Inputs: the project takes only a dump folder that the user made from their own console (`base/`, `updates/<version>/`, `dlc/`, each with `exefs/` and `romfs/`), or a `.nup` package of one. It uses no keys and contains no Nintendo code, firmware or keys. Add nothing that asks for keys or explains how to obtain games.
- Where to start:

  | Task | Read first |
  |---|---|
  | A crash, abort, hang, `[RED]` line, wrong picture, player issue | [`issue_digest.py`](docs/agents/05-debugging.md#players-issues-and-logs) first, then [docs/agents/symptoms.md](docs/agents/symptoms.md): search it for the text in the log or what you see; it leads to the entry in [docs/reference](docs/reference/README.md) and the games it was found on. Recipes are in [05-debugging.md](docs/agents/05-debugging.md) |
  | A run on the phone, its log, switches, saves | [android/TESTING.md](android/TESTING.md) |
  | A game that does not reach its frame loop | [03-bringup.md](docs/agents/03-bringup.md), then the symptom index |
  | Rendering or shaders | [android/SHADERS.md](android/SHADERS.md), [04-graphics.md](docs/agents/04-graphics.md), then [rendering](docs/reference/rendering.md), [shaders](docs/reference/shaders.md) and [NVN objects](docs/reference/nvn-objects.md) |
  | Work on one game, or a change to its profile | its note, `dm/games/<id>/README.md`, then [dm/games/README.md](dm/games/README.md) for the keys |
  | Another title | `scripts/build_ipa_from_nsp.py` on its dump folder (survey: `<dump folder>/<title ID>_survey/survey.md`), then [docs/agents](docs/agents/README.md) |
  | Packing, building, the launcher, GPU drivers | [android/README.md](android/README.md); the upstream tools in [docs/development.md](docs/development.md) |
  | Checking a change before a pull request | [06-dev-loop.md](docs/agents/06-dev-loop.md) |
- Where a new finding goes, once: a fact about one game (its passes, shader keys, measurements, why its profile needs a key or a cut, its status) in that game's note, in the section it belongs to and marked current or resolved; a general mechanism (a host or tool change that holds for other games) as an entry in its `docs/reference/<subsystem>.md` section (Symptom, Cause, Fix, Found on with the game's note linked), then `python3 dm/tools/docs_symptoms.py` to update the symptom index, and a line under the game note's "Found on this game". Link findings; do not repeat them. Adding a game takes its note and its row in the games table. This file changes only when a rule does; it states no game count (the games table is the count). `dm/tools/tests/test_docs.py` checks the links and anchors, the notes and their table rows, the symptom index, and the length of this file (1,500 words) and of each guide in `docs/agents` (6,000).
- Each game's constants and shader tables form a game profile in `dm/games/<id>/`, where `<id>` is the base game's title ID: `game.tsv` plus the tables (and `patches.py` when the rewriter has patches for its builds). Donkey Kong Bananza's profile is `040074A01BF12000`. Tools pick it with `DM_GAME`, `make -C dm/host` and the unit tests with `GAME`, the APK with `-PnxGames`. The tools have no default game: set it every time. Put per-game values in the profile, not in code ([dm/games/README.md](dm/games/README.md)).
- Prefer general optimizations and fixes to game-specific rules. Performance work (frame pacing, power, heat, memory, load times) counts as much as correctness. First look for a change that holds for every game: Horizon / NVN / Vulkan semantics, or what the binary, its SDK version, its imports or its files show. Use a game-specific rule (a profile key, a rewriter cut, a build-ID gate, a per-game branch in the host) only when it is absolutely necessary, that is, when no general rule can do it. Driver differences key on what the driver reports (`driverID`, features), never on its name.
- Find a bug's root cause and fix it there; do not just work around it. Before changing code, find why it happens: the faulting instruction and its decompilation, the value that went wrong and who wrote it, the Horizon / NVN / Vulkan semantics the host or the translator gets wrong. Then fix that place. A change that only hides the symptom (a stub, a skipped call, a clamp, a retry, a sleep or a longer timeout, a cache flush, a profile key that turns a feature off) is a workaround: use one only when the cause is known and lies where it cannot be fixed (the game's own code, which a rewriter cut changes deliberately, or a platform or driver limit), or when the user asks for a stopgap. Then say so in the code comment and in the game note or reference entry: the cause, or what is still unknown, marked open.
- Follow the [upstream technical instructions](AGENTS-UPSTREAM.md) for per-build knowledge and shader translation.
- Scripts the user runs live in `scripts/` and run from a terminal (non-zero exit on error); tools only agents or other scripts call stay under `dm/`.
- Game files and anything extracted from them stay with the game's dump folder, never in this repository or the home folder (nearly full): the work folder `<dump folder>/<id>-work` for extraction, runs and analysis; `pack_android.py` writes `<id>-work/android-pack`. Tool venvs and large scratch data go there too. No local paths, phone serials, private repository names, personal e-mail addresses or internal IP addresses in any committed file.
- Language: English everywhere (docs, code comments, log messages, script output, the app's text). Red log lines (something unimplemented or wrong) start with `[RED] `. Text that must match a game's own Chinese data is written as escapes (`"\xE8\xB5\xB7"`), never as characters.
- Checking a change ([06-dev-loop.md](docs/agents/06-dev-loop.md#checking-a-change-for-android)): the unit tests (`make -C dm/host/tests GAME=<id> run`, and `dm/gfx/tests` where the change reaches graphics), the Python tests in `dm/tools/tests` (they include the launcher's JVM tests, `test_android_*.py`), then short phone runs. Phone runs use the test app `dev.nxnative.android` only, never the play app `dev.nxnative.android.play` or the release; never delete or overwrite a save folder ([Saves](android/TESTING.md#saves)); about 20 s of roaming play per run is enough ([Fast test runs](android/TESTING.md#fast-test-runs)); every round includes a run on a Turnip driver through `DM_VK_DRIVER` ([Custom GPU drivers](android/README.md#custom-gpu-drivers)) as well as the system driver; force-stop the test app afterwards. The pull request says what ran, on which driver, and what was not.
- The Mac and iOS path (Metal host, IPAs) is upstream's; Android work does not need to build or test it.
- Pull requests bring source only: no binary (`dm/gfx/tools/dmsh_xlate_bin`, `.so`, `.dll`, APK, GPU driver), no `dmsh_chain.lock` or chain header, no edit under `dm/third_party/`, no branch carrying commits the public branch no longer has. Every binary is built here from source; vendored code changes only as a documented version bump. Review with [07-pr-review.md](docs/agents/07-pr-review.md) and `dm/tools/pr_risk.py` before any merge.
- Commit only when asked, with this checkout's git identity; public commits come only from the publish scripts.

## Peanuts Pro FreeCam

### Comunicação

- Responda sempre em português brasileiro. O proprietário não é programador e gerencia o projeto pelo celular REDMAGIC 11 Air com 16 GB de RAM e 512 GB de armazenamento.
- Execute as alterações solicitadas diretamente no projeto. Evite pedir ao proprietário que edite código manualmente.
- Explique os resultados de forma simples e objetiva. Forneça links diretos para as compilações e os arquivos.

### Projeto

- Repositório: `joeltutors174-gif/peanuts-native`. Versão estável de referência: Aurora v1.4. Preserve as versões anteriores como backup.
- Preserve o identificador do aplicativo e a assinatura digital permanente. Nunca exponha as chaves de assinatura.
- Preserve os saves, as configurações e os jogos importados.
- Preserve os 76 perfis oficiais anteriores e o catálogo pesquisável em três colunas com capas offline.
- Mantenha a câmera livre configurável individualmente por jogo.
- Preserve o tema grafite e lavanda Aurora. Use a logo P com degradê azul, roxo e rosa.

### Desenvolvimento

- Analise os arquivos antes de modificá-los. Preserve as funcionalidades que já funcionam, salvo quando a alteração exigir mudanças nelas.
- Crie versões separadas para novas melhorias. Valide o código antes de publicar e informe claramente as falhas e os testes pendentes.
- Avance a numeração a cada atualização Aurora (v1.5 → v1.6 → v1.7). Use a mesma versão no app, no APK, no nome do workflow e no título das execuções. Incremente o código de versão do Android. Crie um workflow próprio na branch principal e preserve os nomes e históricos anteriores.
- Use o GitHub Actions para compilar os APKs. Preserve os créditos dos desenvolvedores originais.

### Versões Aurora

- Aurora v1.5 ajustou o ícone externo em 1:1. Aurora v1.6 corrigiu o diagnóstico na tela. Aurora v1.7 usa a base oficial 0.1.9 com 87 perfis.
