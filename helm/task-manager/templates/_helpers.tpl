{{- define "tm.name" -}}task-manager{{- end -}}

{{- define "tm.labels" -}}
app.kubernetes.io/name: {{ include "tm.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Values.image.tag | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

{{- define "tm.selector" -}}
app.kubernetes.io/name: {{ include "tm.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: api
{{- end -}}

{{- define "tm.dbSelector" -}}
app.kubernetes.io/name: {{ include "tm.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: database
{{- end -}}
