#version 330

uniform sampler2D InSampler;
in vec2 texCoord;

layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };
layout(std140) uniform ColorConfig { vec4 Adjust; vec4 HueAndPadding; };
out vec4 fragColor;

vec3 rgb2hsv(vec3 c) {
  vec4 K = vec4(0.0, -1.0/3.0, 2.0/3.0, -1.0);
  vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
  vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
  float d = q.x - min(q.w, q.y);
  float e = 1.0e-10;
  return vec3(abs(q.z + (q.w-q.y)/(6.0*d+e)), d/(q.x+e), q.x);
}

vec3 hsv2rgb(vec3 c) {
  vec3 p = abs(fract(c.xxx + vec3(0.0, 2.0/3.0, 1.0/3.0))*6.0-3.0);
  return c.z * mix(vec3(1.0), clamp(p-1.0, 0.0, 1.0), c.y);
}

void main() {
  vec3 color = texture(InSampler, texCoord).rgb;
  vec3 hsv = rgb2hsv(color);
  hsv.x = fract(hsv.x + HueAndPadding.x / 360.0);
  hsv.y = clamp(hsv.y * Adjust.x, 0.0, 1.0);
  color = hsv2rgb(hsv);
  color = pow(max(color, vec3(0.0)), vec3(1.0 / max(Adjust.w, 0.01)));
  color = ((color - 0.5) * Adjust.y + 0.5) * Adjust.z;
  fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
