if [[ ! -o interactive ]]; then
    return
fi

compctl -K _finatra finatra

_finatra() {
  local word words completions
  read -cA words
  word="${words[2]}"

  if [ "${#words}" -eq 2 ]; then
    completions="$(finatra commands)"
  else
    completions="$(finatra completions "${word}")"
  fi

  reply=("${(ps:\n:)completions}")
}
