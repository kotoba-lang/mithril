#!/bin/bash
# Install this file as <HERMES_HOME>/scripts/mithril-bpmn-adapter.sh.
# The cron job workdir must be the checked-out Mithril runtime containing the
# bound contract. The scheduler CLI validates that workdir and the live claim.
set -euo pipefail

if [[ -z "${HERMES_HOME:-}" || ! -d "${HERMES_HOME}/cron" ]]; then
  printf 'REFUSE\tmissing-hermes-profile\n' >&2
  exit 1
fi
if [[ ! -f "${PWD}/examples/hermes-readonly-scheduler.mith" ]]; then
  printf 'REFUSE\tmissing-bound-contract\n' >&2
  exit 1
fi

profiles_root="$(dirname "${HERMES_HOME}")"
hermes_root="$(dirname "${profiles_root}")"
exec kbb --backend sci "${PWD}/bin/mithril-hermes-scheduler.cljk" tick \
  "${PWD}/examples/hermes-readonly-scheduler.mith" \
  "${PWD}/examples/hermes-readonly-bot.mith" \
  "${profiles_root}" \
  "${hermes_root}/mithril/profile-fleet-v1" \
  "${hermes_root}/mithril/scheduler-state-v1"
