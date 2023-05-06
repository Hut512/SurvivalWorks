package de.survivalworkers.core.client.engine.graphics.shaders;

public class ShaderCompiler {

    /**
     * Compiles the shader if it was changed
     * @param glslShaderFile The file
     * @param shaderType The type
     */
    /*public static void reCompileShader(String glslShaderFile,int shaderType){
        byte[] compiledShader;
        try {
            File glslFile = new File(glslShaderFile);
            File spvFile = new File(glslShaderFile + ".spv");
            if(!spvFile.exists() || glslFile.lastModified() > spvFile.lastModified()){
                String  shaderCode = new String(Files.readAllBytes(glslFile.toPath()));
                compiledShader = compileShader(shaderCode,shaderType);
                Files.write(spvFile.toPath(),compiledShader);
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public static byte[] compileShader(String shaderCode,int shaderType){
        long compiler = 0;
        long options = 0;
        byte[] compiledShader;

        try {
            compiler = Shaderc.shaderc_compiler_initialize();
            options = Shaderc.shaderc_compile_options_initialize();
            long res = Shaderc.shaderc_compile_into_spv(compiler,shaderCode,shaderType,"shader.glsl","main",options);

            if(Shaderc.shaderc_result_get_compilation_status(res) != Shaderc.shaderc_compilation_status_success)throw new RuntimeException("Could not compile Shader: " + Shaderc.shaderc_result_get_error_message(res));

            ByteBuffer buffer = Shaderc.shaderc_result_get_bytes(res);
            compiledShader = new byte[buffer.remaining()];
            buffer.get(compiledShader);
        }finally {
            Shaderc.shaderc_compile_options_release(options);
            Shaderc.shaderc_compiler_release(compiler);
        }

        return compiledShader;
    }*/
}
