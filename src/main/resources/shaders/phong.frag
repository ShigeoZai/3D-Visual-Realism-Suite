#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;

uniform sampler2D textureSampler;   // unit 0
uniform sampler2D shadowMap;        // unit 1
uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadow;

uniform float specularStrength;
uniform float biasMax;              // bias maksimum (permukaan miring terhadap cahaya)
uniform float biasMin;              // bias minimum (permukaan tegak lurus cahaya)

// Mengembalikan 0.0 (terang) .. 1.0 (bayangan penuh)
float ShadowCalculation(vec4 fragPosLS, vec3 norm, vec3 lightDir) {
    vec3 proj = fragPosLS.xyz / fragPosLS.w;   // perspective divide -> NDC
    proj = proj * 0.5 + 0.5;                   // NDC [-1,1] -> [0,1]
    if (proj.z > 1.0) return 0.0;              // di luar far plane cahaya

    float currentDepth = proj.z;
    // Slope-scaled bias: bias = max(biasMax * (1 - N.L), biasMin)
    float bias = max(biasMax * (1.0 - dot(norm, lightDir)), biasMin);

    // PCF 3x3 untuk menghaluskan tepi bayangan
    float shadow = 0.0;
    vec2 texel = 1.0 / vec2(textureSize(shadowMap, 0));
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float closest = texture(shadowMap, proj.xy + vec2(x, y) * texel).r;
            shadow += (currentDepth - bias > closest) ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
}

void main() {
    // 1. Ambient
    vec3 ambient = 0.2 * lightColor;

    // 2. Diffuse (Lambertian)
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular (Phong vs Blinn-Phong)
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (useBlinnPhong) {
        vec3 halfwayDir = normalize(lightDir + viewDir);
        spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);   // Blinn-Phong
    } else {
        vec3 reflectDir = reflect(-lightDir, norm);
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Phong
    }
    if (diff <= 0.0) spec = 0.0;
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Shadow Factor (1.0 = terang, 0.2 = bayangan)
    float shadowFactor = 1.0;
    if (useShadow) {
        float s = ShadowCalculation(FragPosLightSpace, norm, lightDir);
        shadowFactor = mix(1.0, 0.2, s);
    }

    // 5. Warna dasar: tekstur atau warna polos
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 result = (ambient + shadowFactor * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}