#version 150

uniform sampler2D DiffuseSampler;
uniform float GrayFactor;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);

    // 计算灰度值（人眼感知权重）
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));

    // 混合原色和灰度（GrayFactor = 0.4 表示 40% 去饱和）
    vec3 result = mix(color.rgb, vec3(gray), GrayFactor);

    fragColor = vec4(result, color.a);
}
